package za.ac.uj.racinglines.pso;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
public class ParticleSwarmOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(ParticleSwarmOptimizer.class);

  private final PsoConfig config;
  private final Random random;

  public PsoResult optimize(Objective objective) {
    long startTime = System.currentTimeMillis();
    int dim = objective.getDimension();
    double[] lower = objective.getLowerBounds();
    double[] upper = objective.getUpperBounds();

    // Initialize swarm
    double[][] positions = new double[config.getSwarmSize()][dim];
    double[][] velocities = new double[config.getSwarmSize()][dim];
    double[] fitness = new double[config.getSwarmSize()];
    double[][] pbest = new double[config.getSwarmSize()][dim];
    double[] pbestFitness = new double[config.getSwarmSize()];

    // Initialize positions and velocities
    for (int p = 0; p < config.getSwarmSize(); p++) {
      for (int d = 0; d < dim; d++) {
        positions[p][d] = lower[d] + (upper[d] - lower[d]) * random.nextDouble();
        velocities[p][d] = (upper[d] - lower[d]) * (random.nextDouble() - 0.5);
        velocities[p][d] = Math.max(-config.getVmax(), Math.min(config.getVmax(), velocities[p][d]));
      }
      pbest[p] = positions[p].clone();
      fitness[p] = objective.evaluate(positions[p]);
      pbestFitness[p] = fitness[p];
    }

    // Find initial global best
    int gbestIdx = 0;
    for (int p = 1; p < config.getSwarmSize(); p++) {
      if (fitness[p] < fitness[gbestIdx]) {
        gbestIdx = p;
      }
    }
    double[] gbest = pbest[gbestIdx].clone();
    double gbestFitness = pbestFitness[gbestIdx];
    int lastImprovementIter = 0;

    long evaluations = config.getSwarmSize();

    logger.info("PSO config", kv("swarm_size", config.getSwarmSize()),
        kv("w_max", config.getWmax()), kv("w_min", config.getWmin()),
        kv("c1", config.getC1()), kv("c2", config.getC2()),
        kv("vmax", config.getVmax()), kv("budget", config.getBudget()));

    List<PsoResult.IterationSnapshot> history = new ArrayList<>();
    int iteration = 0;
    String terminationReason = "budget";

    while (evaluations < config.getBudget()) {
      iteration++;

      // Inertia weight schedule (linear decrease)
      double w = config.getWmax() - (config.getWmax() - config.getWmin()) * iteration / (config.getBudget() / config.getSwarmSize());

      // Update velocities and positions
      int velocityClampedCount = 0;
      for (int p = 0; p < config.getSwarmSize(); p++) {
        for (int d = 0; d < dim; d++) {
          double r1 = random.nextDouble();
          double r2 = random.nextDouble();
          velocities[p][d] = w * velocities[p][d]
              + config.getC1() * r1 * (pbest[p][d] - positions[p][d])
              + config.getC2() * r2 * (gbest[d] - positions[p][d]);

          if (Math.abs(velocities[p][d]) > config.getVmax()) {
            velocities[p][d] = Math.max(-config.getVmax(), Math.min(config.getVmax(), velocities[p][d]));
            velocityClampedCount++;
          }
        }

        // Update position and clamp to bounds
        for (int d = 0; d < dim; d++) {
          positions[p][d] += velocities[p][d];
          if (positions[p][d] < lower[d]) {
            positions[p][d] = lower[d];
            velocities[p][d] = 0.0;
          } else if (positions[p][d] > upper[d]) {
            positions[p][d] = upper[d];
            velocities[p][d] = 0.0;
          }
        }

        // Evaluate fitness
        fitness[p] = objective.evaluate(positions[p]);
        evaluations++;

        // Update personal best (monotonic - never gets worse)
        if (fitness[p] < pbestFitness[p]) {
          pbest[p] = positions[p].clone();
          pbestFitness[p] = fitness[p];
        }

        // Update global best
        if (pbestFitness[p] < gbestFitness) {
          gbest = pbest[p].clone();
          gbestFitness = pbestFitness[p];
          lastImprovementIter = iteration;

          logger.info("New best", kv("iter", iteration),
              kv("lap_s", gbestFitness),
              kv("improvement_s", gbestFitness),
              kv("gap_to_baseline", gbestFitness));
        }

        logger.debug("Particle update", kv("particle_id", p),
            kv("fitness", fitness[p]),
            kv("pbest", pbestFitness[p]),
            kv("velocity_norm", Math.sqrt(dotProduct(velocities[p], velocities[p]))));
      }

      if (velocityClampedCount > 0) {
        logger.debug("Velocity clamped", kv("count", velocityClampedCount));
      }

      // Calculate diversity
      double[] centroid = new double[dim];
      for (int p = 0; p < config.getSwarmSize(); p++) {
        for (int d = 0; d < dim; d++) {
          centroid[d] += positions[p][d];
        }
      }
      for (int d = 0; d < dim; d++) {
        centroid[d] /= config.getSwarmSize();
      }

      double diversity = 0.0;
      for (int p = 0; p < config.getSwarmSize(); p++) {
        double dist = 0.0;
        for (int d = 0; d < dim; d++) {
          dist += (positions[p][d] - centroid[d]) * (positions[p][d] - centroid[d]);
        }
        diversity += Math.sqrt(dist);
      }
      diversity /= config.getSwarmSize();

      // Calculate mean fitness
      double meanFitness = 0.0;
      for (int p = 0; p < config.getSwarmSize(); p++) {
        meanFitness += pbestFitness[p];
      }
      meanFitness /= config.getSwarmSize();

      history.add(new PsoResult.IterationSnapshot(iteration, evaluations, gbestFitness, meanFitness, diversity, w));

      logger.info("Iteration end", kv("iter", iteration), kv("evals", evaluations),
          kv("gbest_lap", gbestFitness), kv("mean_lap", meanFitness),
          kv("diversity", diversity), kv("w", w));

      // Stagnation check
      if (iteration - lastImprovementIter >= 50) {
        terminationReason = "stagnation";
        logger.warn("Stagnation", kv("no_improvement_for", iteration - lastImprovementIter));
        break;
      }

      if (evaluations >= config.getBudget()) {
        break;
      }
    }

    long durationMs = System.currentTimeMillis() - startTime;

    logger.info("Run end", kv("best_lap", gbestFitness),
        kv("evals_used", evaluations), kv("duration_s", durationMs / 1000.0),
        kv("reason", terminationReason));

    return PsoResult.builder()
        .bestVector(gbest)
        .bestFitness(gbestFitness)
        .evaluationsUsed(evaluations)
        .durationMs(durationMs)
        .terminationReason(terminationReason)
        .history(history)
        .build();
  }

  private double dotProduct(double[] a, double[] b) {
    double result = 0.0;
    for (int i = 0; i < a.length; i++) {
      result += a[i] * b[i];
    }
    return result;
  }
}
