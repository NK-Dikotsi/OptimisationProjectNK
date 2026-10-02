package za.ac.uj.racinglines.fixtures;

import za.ac.uj.racinglines.pso.Objective;
import za.ac.uj.racinglines.pso.PsoConfig;

public class Pso {
  public static PsoConfig standardConfig() {
    return new PsoConfig(20, 0.7, 1.49618, 1.49618, 2.0, 5000);
  }

  public static class SphereObjective implements Objective {
    private final int dimension;

    public SphereObjective(int dimension) {
      this.dimension = dimension;
    }

    @Override
    public double evaluate(double[] x) {
      double sum = 0.0;
      for (double v : x) {
        sum += v * v;
      }
      return sum;
    }

    @Override
    public int getDimension() {
      return dimension;
    }

    @Override
    public double[] getLowerBounds() {
      double[] bounds = new double[dimension];
      for (int i = 0; i < dimension; i++) {
        bounds[i] = -5.12;
      }
      return bounds;
    }

    @Override
    public double[] getUpperBounds() {
      double[] bounds = new double[dimension];
      for (int i = 0; i < dimension; i++) {
        bounds[i] = 5.12;
      }
      return bounds;
    }
  }
}
