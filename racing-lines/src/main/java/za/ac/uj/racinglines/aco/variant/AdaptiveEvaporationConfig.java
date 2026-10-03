package za.ac.uj.racinglines.aco.variant;

import lombok.Getter;
import za.ac.uj.racinglines.aco.AcoConfig;

@Getter
public class AdaptiveEvaporationConfig extends AcoConfig {

  /** Which way evaporation responds to pheromone entropy. */
  public enum RhoRule {
    /** Low entropy (converged) raises rho: forget faster once the colony agrees. */
    RAISE_WHEN_CONVERGED,
    /** Low entropy (converged) lowers rho: forget more slowly once the colony agrees. */
    LOWER_WHEN_CONVERGED
  }

  private final double rhoMin;
  private final double rhoMax;
  private final RhoRule rule;

  public AdaptiveEvaporationConfig(int numAnts, int numNodes, double alpha, double beta,
                                   double rhoMin, double rhoMax, double q, double tau0,
                                   long budget) {
    super(numAnts, numNodes, alpha, beta, (rhoMin + rhoMax) / 2, q, tau0, budget);
    this.rhoMin = rhoMin;
    this.rhoMax = rhoMax;
    this.rule = RhoRule.RAISE_WHEN_CONVERGED;
  }

  public AdaptiveEvaporationConfig(int numAnts, int numNodes, double alpha, double beta,
                                   double rhoMin, double rhoMax, double q, double tau0,
                                   double tauMin, double tauMax, long budget) {
    this(numAnts, numNodes, alpha, beta, rhoMin, rhoMax, q, tau0, tauMin, tauMax, budget,
        RhoRule.RAISE_WHEN_CONVERGED);
  }

  public AdaptiveEvaporationConfig(int numAnts, int numNodes, double alpha, double beta,
                                   double rhoMin, double rhoMax, double q, double tau0,
                                   double tauMin, double tauMax, long budget, RhoRule rule) {
    super(numAnts, numNodes, alpha, beta, (rhoMin + rhoMax) / 2, q, tau0, tauMin, tauMax, budget);
    this.rhoMin = rhoMin;
    this.rhoMax = rhoMax;
    this.rule = rule;
  }
}
