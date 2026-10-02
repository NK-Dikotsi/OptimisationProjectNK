package za.ac.uj.racinglines.aco;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AcoConfig {
  private final int numAnts;
  private final int numNodes;
  private final double alpha;
  private final double beta;
  private final double rho;
  private final double q;
  private final double tau0;
  private final double tauMin;
  private final double tauMax;
  private final long budget;

  public AcoConfig(int numAnts, int numNodes, double alpha, double beta,
                   double rho, double q, double tau0, long budget) {
    this(numAnts, numNodes, alpha, beta, rho, q, tau0, tau0 * 0.5, tau0 * 2.0, budget);
  }
}
