package za.ac.uj.racinglines.aco.variant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.ac.uj.racinglines.aco.AcoConfig;
import za.ac.uj.racinglines.aco.AcoResult;
import za.ac.uj.racinglines.aco.LineObjective;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
public class AdaptiveEvaporationOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(AdaptiveEvaporationOptimizer.class);

  private final AdaptiveEvaporationConfig config;
  private final Random random;

  public AcoResult optimize(LineObjective objective) {
    long startTime = System.currentTimeMillis();
    int numGates = objective.getNumGates();
    int numNodes = objective.getNumNodes();
    double halfWidth = objective.getHalfWidth();

    double[][] tau = new double[numGates][numNodes];
    for (int i = 0; i < numGates; i++) {
      for (int j = 0; j < numNodes; j++) {
        tau[i][j] = config.getTau0();
      }
    }

    double[] nodeOffsets = new double[numNodes];
    for (int k = 0; k < numNodes; k++) {
      if (numNodes == 1) {
        nodeOffsets[k] = 0.0;
      } else {
        nodeOffsets[k] = -halfWidth + (2.0 * halfWidth * k) / (numNodes - 1);
      }
    }

    int[] bestLine = null;
    double bestLap = Double.POSITIVE_INFINITY;
    long evaluations = 0;
    double currentRho = (config.getRhoMin() + config.getRhoMax()) / 2;

    logger.info("Variant config", kv("option", "adaptive_evaporation"),
        kv("rho_min", config.getRhoMin()), kv("rho_max", config.getRhoMax()),
        kv("ants", config.getNumAnts()), kv("K", config.getNumNodes()),
        kv("alpha", config.getAlpha()), kv("beta", config.getBeta()),
        kv("Q", config.getQ()), kv("tau0", config.getTau0()),
        kv("tau_min", config.getTauMin()), kv("tau_max", config.getTauMax()),
        kv("budget", config.getBudget()));

    List<AcoResult.IterationSnapshot> history = new ArrayList<>();
    int iteration = 0;
    String terminationReason = "budget";

    double maxEntropy = Math.log(numNodes);

    while (evaluations < config.getBudget()) {
      iteration++;

      int[][] antLines = new int[config.getNumAnts()][numGates];
      double[] antLaps = new double[config.getNumAnts()];

      for (int ant = 0; ant < config.getNumAnts(); ant++) {
        antLines[ant] = buildAntLine(tau, nodeOffsets, numGates, numNodes);
        antLaps[ant] = objective.evaluateLine(antLines[ant]);
        evaluations++;

        if (antLaps[ant] < bestLap) {
          bestLap = antLaps[ant];
          bestLine = antLines[ant].clone();

          logger.info("New best", kv("iter", iteration),
              kv("lap_s", bestLap),
              kv("improvement_s", bestLap),
              kv("gap_to_baseline", bestLap));
        }

        int lateralJumps = 0;
        for (int i = 1; i < numGates; i++) {
          double diff = Math.abs(nodeOffsets[antLines[ant][i]] - nodeOffsets[antLines[ant][i - 1]]);
          if (diff > halfWidth / 2) {
            lateralJumps++;
          }
        }

        logger.debug("Ant built", kv("ant_id", ant),
            kv("lap_s", antLaps[ant]),
            kv("lateral_jumps", lateralJumps));
      }

      evaporatePheromone(tau, currentRho, config.getTauMin(), config.getTauMax());

      int tauBoundHits = 0;
      for (int i = 0; i < numGates; i++) {
        for (int j = 0; j < numNodes; j++) {
          if (tau[i][j] <= config.getTauMin() || tau[i][j] >= config.getTauMax()) {
            tauBoundHits++;
          }
        }
      }

      if (tauBoundHits > 0) {
        logger.debug("Tau bound hit", kv("count", tauBoundHits));
      }

      // Calculate iteration best lap and mean lap first
      double iterationBestLap = Double.POSITIVE_INFINITY;
      int bestAntThisIteration = -1;
      double meanLap = 0.0;
      for (int ant = 0; ant < config.getNumAnts(); ant++) {
        if (antLaps[ant] < iterationBestLap) {
          iterationBestLap = antLaps[ant];
          bestAntThisIteration = ant;
        }
        meanLap += antLaps[ant];
      }
      meanLap /= config.getNumAnts();

      // MAX-MIN Ant System: only the iteration best ant deposits pheromone
      depositPheromone(tau, antLines[bestAntThisIteration], antLaps[bestAntThisIteration],
          config.getQ(), config.getTauMin(), config.getTauMax());

      double entropy = calculatePheromoneEntropy(tau, numGates, numNodes);

      // Adaptive evaporation: when entropy is low, increase rho; when high, decrease rho
      double normalizedEntropy = entropy / maxEntropy;
      double previousRho = currentRho;
      currentRho = config.getRhoMin() + (config.getRhoMax() - config.getRhoMin()) * (1.0 - normalizedEntropy);
      currentRho = Math.max(config.getRhoMin(), Math.min(config.getRhoMax(), currentRho));

      if (previousRho != currentRho) {
        logger.info("rho_update", kv("iter", iteration),
            kv("entropy", entropy),
            kv("prev_rho", previousRho),
            kv("new_rho", currentRho));
      }

      double tauMinVal = Double.POSITIVE_INFINITY;
      double tauMaxVal = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < numGates; i++) {
        for (int j = 0; j < numNodes; j++) {
          tauMinVal = Math.min(tauMinVal, tau[i][j]);
          tauMaxVal = Math.max(tauMaxVal, tau[i][j]);
        }
      }

      history.add(new AcoResult.IterationSnapshot(iteration, evaluations, bestLap,
          iterationBestLap, meanLap, entropy, tauMinVal, tauMaxVal));

      logger.info("iteration_end", kv("iter", iteration), kv("evals", evaluations),
          kv("best_lap", bestLap), kv("iter_best_lap", iterationBestLap),
          kv("mean_lap", meanLap), kv("entropy", entropy),
          kv("tau_min", tauMinVal), kv("tau_max", tauMaxVal),
          kv("current_rho", currentRho));

      if (evaluations >= config.getBudget()) {
        break;
      }
    }

    long durationMs = System.currentTimeMillis() - startTime;

    logger.info("Run end", kv("best_lap", bestLap),
        kv("evals_used", evaluations), kv("duration_s", durationMs / 1000.0),
        kv("reason", terminationReason));

    return AcoResult.builder()
        .bestLine(bestLine)
        .bestLap(bestLap)
        .evaluationsUsed(evaluations)
        .durationMs(durationMs)
        .terminationReason(terminationReason)
        .history(history)
        .build();
  }

  private int[] buildAntLine(double[][] tau, double[] nodeOffsets, int numGates, int numNodes) {
    int[] line = new int[numGates];
    int prevNode = numNodes / 2;

    for (int gate = 0; gate < numGates; gate++) {
      double[] probabilities = calculateTransitionProbabilities(tau[gate], nodeOffsets,
                                                                prevNode, numNodes);
      line[gate] = rouletteWheelSelect(probabilities);
      prevNode = line[gate];
    }

    return line;
  }

  private double[] calculateTransitionProbabilities(double[] tauRow, double[] nodeOffsets,
                                                   int prevNode, int numNodes) {
    double[] probs = new double[numNodes];
    double sum = 0.0;

    for (int j = 0; j < numNodes; j++) {
      double heuristic = 1.0 / (1.0 + Math.abs(nodeOffsets[j] - nodeOffsets[prevNode]));
      double tauAlpha = Math.pow(tauRow[j], config.getAlpha());
      double etaBeta = Math.pow(heuristic, config.getBeta());
      probs[j] = tauAlpha * etaBeta;
      sum += probs[j];
    }

    for (int j = 0; j < numNodes; j++) {
      probs[j] /= sum;
    }

    return probs;
  }

  private int rouletteWheelSelect(double[] probabilities) {
    double r = random.nextDouble();
    double sum = 0.0;
    for (int i = 0; i < probabilities.length; i++) {
      sum += probabilities[i];
      if (r <= sum) {
        return i;
      }
    }
    return probabilities.length - 1;
  }

  private void evaporatePheromone(double[][] tau, double rho, double tauMin, double tauMax) {
    for (int i = 0; i < tau.length; i++) {
      for (int j = 0; j < tau[i].length; j++) {
        tau[i][j] = tau[i][j] * (1.0 - rho);
        tau[i][j] = Math.max(tauMin, Math.min(tauMax, tau[i][j]));
      }
    }
  }

  private void depositPheromone(double[][] tau, int[] line, double lap, double q,
                               double tauMin, double tauMax) {
    double deposit = q / lap;
    for (int i = 0; i < line.length; i++) {
      tau[i][line[i]] += deposit;
      tau[i][line[i]] = Math.max(tauMin, Math.min(tauMax, tau[i][line[i]]));
    }
  }

  private double calculatePheromoneEntropy(double[][] tau, int numGates, int numNodes) {
    double totalEntropy = 0.0;

    for (int i = 0; i < numGates; i++) {
      double rowSum = 0.0;
      for (int j = 0; j < numNodes; j++) {
        rowSum += tau[i][j];
      }

      double gateEntropy = 0.0;
      for (int j = 0; j < numNodes; j++) {
        double prob = tau[i][j] / rowSum;
        if (prob > 0) {
          gateEntropy -= prob * Math.log(prob);
        }
      }
      totalEntropy += gateEntropy;
    }

    return totalEntropy / numGates;
  }
}
