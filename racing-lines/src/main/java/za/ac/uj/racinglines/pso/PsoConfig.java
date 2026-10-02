package za.ac.uj.racinglines.pso;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PsoConfig {
  private final int swarmSize;
  private final double w;
  private final double wmax;
  private final double wmin;
  private final double c1;
  private final double c2;
  private final double vmax;
  private final long budget;

  public PsoConfig(int swarmSize, double w, double c1, double c2, double vmax, long budget) {
    this(swarmSize, w, w, w, c1, c2, vmax, budget);
  }
}
