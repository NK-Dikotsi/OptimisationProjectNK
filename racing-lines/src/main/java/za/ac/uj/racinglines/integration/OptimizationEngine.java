package za.ac.uj.racinglines.integration;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.ac.uj.racinglines.aco.AcoConfig;
import za.ac.uj.racinglines.aco.AcoResult;
import za.ac.uj.racinglines.aco.AntColonyOptimizer;
import za.ac.uj.racinglines.aco.variant.AdaptiveEvaporationConfig;
import za.ac.uj.racinglines.aco.variant.AdaptiveEvaporationOptimizer;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.pso.ParticleSwarmOptimizer;
import za.ac.uj.racinglines.pso.PsoConfig;
import za.ac.uj.racinglines.pso.PsoResult;
import za.ac.uj.racinglines.track.Track;

/**
 * Runs one algorithm on one track against the real lap-time physics.
 *
 * <p>All three algorithms minimise the same {@link LapTimeObjective}. ACO and adaptive
 * evaporation see it through {@link NodeLapTimeObjective}. Every reported best lap is
 * re-verified against the line the optimiser returned, so a mismatch fails loudly.
 */
public class OptimizationEngine {
  private static final Logger logger = LoggerFactory.getLogger(OptimizationEngine.class);

  /** Algorithm parameters. Same population size for every algorithm for a fair comparison. */
  public record Params(
      int population,
      int acoNodes,
      double alpha,
      double beta,
      double rho,
      double rhoMin,
      double rhoMax,
      double inertia,
      double c1,
      double c2,
      double vmaxFractionOfWidth,
      int controlPoints) {

    public static Params defaults() {
      // 20 control points: one every ~25 m on a 500 m track, splined to all 100 gates.
      return new Params(20, 11, 1.0, 2.0, 0.1, 0.05, 0.3, 0.7298, 1.49618, 1.49618, 0.2, 20);
    }
  }

  private final LapTimeSimulator simulator;
  private final Params params;

  public OptimizationEngine(LapTimeSimulator simulator, Params params) {
    this.simulator = simulator;
    this.params = params;
  }

  public RunOutcome optimize(String algorithm, String trackName, Track track, double trackWidth,
      long budget, long seed) {
    LapTimeObjective lapTime =
        new LapTimeObjective(track, simulator, trackWidth, params.controlPoints());
    double baseline = lapTime.baseline();
    Random random = new Random(seed);

    logger.info("optimization_start", kv("algorithm", algorithm), kv("track", trackName),
        kv("budget", budget), kv("seed", seed), kv("baseline_lap", baseline));

    RunOutcome outcome = switch (algorithm) {
      case "pso" -> runPso(trackName, lapTime, baseline, budget, random);
      case "aco" -> runAco(trackName, lapTime, baseline, budget, random, false);
      case "adaptive_evaporation" -> runAco(trackName, lapTime, baseline, budget, random, true);
      default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
    };

    if (outcome.evaluationsUsed() > budget) {
      throw new IllegalStateException(
          "Budget exceeded: " + outcome.evaluationsUsed() + " > " + budget);
    }

    logger.info("optimization_end", kv("algorithm", algorithm), kv("track", trackName),
        kv("best_lap", outcome.bestLap()), kv("baseline_lap", baseline),
        kv("improvement_pct", outcome.improvementPct()),
        kv("evals_used", outcome.evaluationsUsed()), kv("evals_to_1pct", outcome.evalsTo1Pct()));
    return outcome;
  }

  private RunOutcome runPso(String trackName, LapTimeObjective lapTime, double baseline,
      long budget, Random random) {
    double width = 2.0 * lapTime.halfWidth();
    PsoConfig config = new PsoConfig(params.population(), params.inertia(), params.c1(),
        params.c2(), params.vmaxFractionOfWidth() * width, budget);
    PsoResult result = new ParticleSwarmOptimizer(config, random).optimize(lapTime);

    double[] best = result.getBestVector();
    verify(lapTime, best, result.getBestFitness(), "pso");

    List<RunOutcome.Point> history = new ArrayList<>();
    for (PsoResult.IterationSnapshot s : result.getHistory()) {
      history.add(new RunOutcome.Point(s.getEvaluations(), s.getGbestFitness()));
    }
    return outcome("pso", trackName, result.getBestFitness(), baseline, best, lapTime, history);
  }

  private RunOutcome runAco(String trackName, LapTimeObjective lapTime, double baseline,
      long budget, Random random, boolean adaptive) {
    NodeLapTimeObjective nodes = new NodeLapTimeObjective(lapTime, params.acoNodes());
    // MAX-MIN bounds: ratio of 100 allows adaptive evaporation to have meaningful room to act.
    double q = baseline;
    double tauMax = 10.0;
    double tauMin = 0.1;
    double tau0 = tauMax;

    AcoResult result;
    String name;
    if (adaptive) {
      AdaptiveEvaporationConfig config = new AdaptiveEvaporationConfig(params.population(),
          params.acoNodes(), params.alpha(), params.beta(), params.rhoMin(), params.rhoMax(),
          q, tau0, tauMin, tauMax, budget);
      result = new AdaptiveEvaporationOptimizer(config, random).optimize(nodes);
      name = "adaptive_evaporation";
    } else {
      AcoConfig config = new AcoConfig(params.population(), params.acoNodes(), params.alpha(),
          params.beta(), params.rho(), q, tau0, tauMin, tauMax, budget);
      result = new AntColonyOptimizer(config, random).optimize(nodes);
      name = "aco";
    }

    double[] best = nodes.toOffsets(result.getBestLine());
    verify(lapTime, best, result.getBestLap(), name);

    List<RunOutcome.Point> history = new ArrayList<>();
    for (AcoResult.IterationSnapshot s : result.getHistory()) {
      history.add(new RunOutcome.Point(s.getEvaluations(), s.getBestLap()));
    }
    return outcome(name, trackName, result.getBestLap(), baseline, best, lapTime, history);
  }

  private static RunOutcome outcome(String algorithm, String trackName, double bestLap,
      double baseline, double[] best, LapTimeObjective lapTime, List<RunOutcome.Point> history) {
    if (history.isEmpty()) {
      throw new IllegalStateException(algorithm + " returned no convergence history");
    }
    long evalsTo1Pct = RunOutcome.evalsTo1Pct(history, bestLap);
    // Report the line per gate (spline-expanded), so best_line.csv can be plotted directly.
    return new RunOutcome(algorithm, trackName, bestLap, baseline, lapTime.toGateOffsets(best),
        lapTime.evaluationsUsed(), evalsTo1Pct, history);
  }

  /** The reported best lap must be the lap of the returned line. */
  private static void verify(LapTimeObjective lapTime, double[] line, double reported,
      String algorithm) {
    double actual = lapTime.lapTimeUncounted(line);
    if (Math.abs(actual - reported) > 1e-6 * Math.max(1.0, actual)) {
      throw new IllegalStateException(algorithm + " reported best lap " + reported
          + " but its best line actually laps in " + actual);
    }
  }
}
