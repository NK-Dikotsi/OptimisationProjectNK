package za.ac.uj.racinglines.integration;

import za.ac.uj.racinglines.pso.Objective;
import za.ac.uj.racinglines.track.RacingTrack;

public class RacingLineObjective implements Objective {
  private final RacingTrack track;
  private final int dimension;
  private final double[] lowerBounds;
  private final double[] upperBounds;

  public RacingLineObjective(RacingTrack track) {
    this.track = track;
    this.dimension = track.getNumGates();
    this.lowerBounds = new double[dimension];
    this.upperBounds = new double[dimension];

    for (int i = 0; i < dimension; i++) {
      lowerBounds[i] = -track.getHalfWidth();
      upperBounds[i] = track.getHalfWidth();
    }
  }

  @Override
  public double evaluate(double[] x) {
    if (x.length != dimension) {
      return Double.POSITIVE_INFINITY;
    }

    int[] line = new int[dimension];
    for (int i = 0; i < dimension; i++) {
      double offset = x[i];
      int bestNode = findBestNode(offset, i);
      line[i] = bestNode;
    }

    return track.evaluateLine(line);
  }

  private int bestNode(double offset, int gateIndex) {
    int numNodes = track.getNumNodes();
    int bestNode = 0;
    double bestDistance = Double.POSITIVE_INFINITY;

    for (int node = 0; node < numNodes; node++) {
      double nodeOffset = track.getGates().get(gateIndex).getNodeOffset(node, numNodes);
      double distance = Math.abs(nodeOffset - offset);
      if (distance < bestDistance) {
        bestDistance = distance;
        bestNode = node;
      }
    }

    return bestNode;
  }

  private int findBestNode(double offset, int gateIndex) {
    return bestNode(offset, gateIndex);
  }

  @Override
  public int getDimension() {
    return dimension;
  }

  @Override
  public double[] getLowerBounds() {
    return lowerBounds;
  }

  @Override
  public double[] getUpperBounds() {
    return upperBounds;
  }

  public RacingTrack getTrack() {
    return track;
  }
}
