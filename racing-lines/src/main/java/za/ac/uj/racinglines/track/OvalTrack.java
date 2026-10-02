package za.ac.uj.racinglines.track;

import java.util.ArrayList;
import java.util.List;

public class OvalTrack implements RacingTrack {
  private static final String NAME = "oval";
  private static final int NUM_GATES = 20;
  private static final int NUM_NODES = 7;
  private static final double HALF_WIDTH = 3.0;
  private static final double CENTRELINE_LAP_TIME = 60.0;

  private final List<Gate> gates;

  public OvalTrack() {
    gates = new ArrayList<>();
    for (int i = 0; i < NUM_GATES; i++) {
      double centerlineY = (double) i / NUM_GATES * 100;
      gates.add(new Gate(i, centerlineY, HALF_WIDTH));
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public int getNumGates() {
    return NUM_GATES;
  }

  @Override
  public int getNumNodes() {
    return NUM_NODES;
  }

  @Override
  public double getHalfWidth() {
    return HALF_WIDTH;
  }

  @Override
  public List<Gate> getGates() {
    return gates;
  }

  @Override
  public double getCentrelineLapTime() {
    return CENTRELINE_LAP_TIME;
  }

  @Override
  public double evaluateLine(int[] line) {
    if (line.length != NUM_GATES) {
      return Double.POSITIVE_INFINITY;
    }

    double totalLapTime = 0.0;
    int[] previousNode = {NUM_NODES / 2};

    for (int gateIdx = 0; gateIdx < NUM_GATES; gateIdx++) {
      int nodeIdx = line[gateIdx];
      Gate gate = gates.get(gateIdx);

      if (nodeIdx < 0 || nodeIdx >= NUM_NODES) {
        return Double.POSITIVE_INFINITY;
      }

      double offset = gate.getNodeOffset(nodeIdx, NUM_NODES);
      double previousOffset = gates.get(gateIdx == 0 ? NUM_GATES - 1 : gateIdx - 1)
          .getNodeOffset(previousNode[0], NUM_NODES);

      double lateralDistance = Math.abs(offset - previousOffset);
      double segmentTime = 1.0 + (lateralDistance / HALF_WIDTH) * 0.2;
      totalLapTime += segmentTime;

      previousNode[0] = nodeIdx;
    }

    return totalLapTime;
  }

  @Override
  public double getSpeedProfile(int gateIndex, double offset) {
    // Speed decreases with lateral distance from center
    double maxSpeed = 50.0;
    double lateralFactor = 1.0 - Math.abs(offset) / HALF_WIDTH * 0.3;
    return maxSpeed * lateralFactor;
  }
}
