package za.ac.uj.racinglines.fixtures;

import za.ac.uj.racinglines.aco.AcoConfig;
import za.ac.uj.racinglines.aco.LineObjective;

public class Aco {
  public static AcoConfig standardConfig() {
    return new AcoConfig(10, 5, 1.0, 1.0, 0.1, 1.0, 1.0, 1000);
  }

  public static class ToyDiscreteObjective implements LineObjective {
    private final int numGates;
    private final int numNodes;
    private final double halfWidth;
    private final int optimalNode;

    public ToyDiscreteObjective(int numGates, int numNodes, double halfWidth, int optimalNode) {
      this.numGates = numGates;
      this.numNodes = numNodes;
      this.halfWidth = halfWidth;
      this.optimalNode = optimalNode;
    }

    @Override
    public double evaluateLine(int[] line) {
      double cost = 0.0;
      for (int node : line) {
        if (node != optimalNode) {
          cost += Math.abs(node - optimalNode);
        }
      }
      return cost;
    }

    @Override
    public int getNumGates() {
      return numGates;
    }

    @Override
    public int getNumNodes() {
      return numNodes;
    }

    @Override
    public double getHalfWidth() {
      return halfWidth;
    }
  }
}
