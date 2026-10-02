package za.ac.uj.racinglines.pso;

public interface Objective {
  double evaluate(double[] x);

  int getDimension();

  double[] getLowerBounds();

  double[] getUpperBounds();
}
