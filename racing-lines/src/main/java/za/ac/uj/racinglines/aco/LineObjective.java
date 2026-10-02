package za.ac.uj.racinglines.aco;

public interface LineObjective {
  double evaluateLine(int[] line);

  int getNumGates(); // N

  int getNumNodes(); // K

  double getHalfWidth(); // w/2 for node spacing
}
