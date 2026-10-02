package za.ac.uj.racinglines.integration;

import za.ac.uj.racinglines.aco.LineObjective;

/**
 * Adapts {@link LapTimeObjective} for ACO: each gate has K discrete nodes evenly spaced over
 * [-w/2, +w/2], and an ant's line (one node index per gate) is converted to offsets before the
 * same lap-time objective is evaluated. Every evaluation is counted by the wrapped objective.
 */
public final class NodeLapTimeObjective implements LineObjective {
  private final LapTimeObjective lapTime;
  private final int numNodes;

  public NodeLapTimeObjective(LapTimeObjective lapTime, int numNodes) {
    if (numNodes < 2) {
      throw new IllegalArgumentException("Need at least 2 nodes per gate, got " + numNodes);
    }
    this.lapTime = lapTime;
    this.numNodes = numNodes;
  }

  @Override
  public double evaluateLine(int[] line) {
    return lapTime.evaluate(toOffsets(line));
  }

  /** Node index to lateral offset. Node 0 = -w/2, node K-1 = +w/2, middle node = 0 when K is odd. */
  public double nodeOffset(int node) {
    double hw = lapTime.halfWidth();
    return -hw + node * (2.0 * hw / (numNodes - 1));
  }

  public double[] toOffsets(int[] line) {
    if (line.length != getNumGates()) {
      throw new IllegalArgumentException(
          "Expected " + getNumGates() + " nodes, got " + line.length);
    }
    double[] offsets = new double[line.length];
    for (int i = 0; i < line.length; i++) {
      if (line[i] < 0 || line[i] >= numNodes) {
        throw new IllegalArgumentException("Node " + line[i] + " out of range at gate " + i);
      }
      offsets[i] = nodeOffset(line[i]);
    }
    return offsets;
  }

  @Override
  public int getNumGates() {
    return lapTime.getDimension();
  }

  @Override
  public int getNumNodes() {
    return numNodes;
  }

  @Override
  public double getHalfWidth() {
    return lapTime.halfWidth();
  }
}
