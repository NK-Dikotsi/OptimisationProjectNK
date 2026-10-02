package za.ac.uj.racinglines.track;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Gate {
  private final int gateNumber;
  private final double centerlineY;
  private final double halfWidth;

  public double getMinOffset() {
    return -halfWidth;
  }

  public double getMaxOffset() {
    return halfWidth;
  }

  public double getNodeOffset(int nodeIndex, int numNodes) {
    if (numNodes == 1) {
      return 0.0;
    }
    return -halfWidth + (2.0 * halfWidth * nodeIndex) / (numNodes - 1);
  }
}
