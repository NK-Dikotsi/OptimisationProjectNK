package za.ac.uj.racinglines.aco.variant;

import lombok.Getter;
import za.ac.uj.racinglines.aco.AcoConfig;

@Getter
public class AdaptiveEvaporationConfig extends AcoConfig {
  private final double rhoMin;
  private final double rhoMax;

  public AdaptiveEvaporationConfig(int numAnts, int numNodes, double alpha, double beta,
                                   double rhoMin, double rhoMax, double q, double tau0,
                                   long budget) {
    super(numAnts, numNodes, alpha, beta, (rhoMin + rhoMax) / 2, q, tau0, budget);
    this.rhoMin = rhoMin;
    this.rhoMax = rhoMax;
  }

  public AdaptiveEvaporationConfig(int numAnts, int numNodes, double alpha, double beta,
                                   double rhoMin, double rhoMax, double q, double tau0,
                                   double tauMin, double tauMax, long budget) {
    super(numAnts, numNodes, alpha, beta, (rhoMin + rhoMax) / 2, q, tau0, tauMin, tauMax, budget);
    this.rhoMin = rhoMin;
    this.rhoMax = rhoMax;
  }
}
