package za.ac.uj.racinglines.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OptimizationResult {
  private String algorithm;
  private String track;
  private int[] bestLine;
  private double bestLap;
  private long evaluationsUsed;
  private long durationMs;
  private double baselineLap;

  public double getImprovementPct() {
    if (baselineLap == 0) {
      return 0.0;
    }
    return ((baselineLap - bestLap) / baselineLap) * 100.0;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public String getTrack() {
    return track;
  }

  public double getBestLap() {
    return bestLap;
  }

  public long getEvaluationsUsed() {
    return evaluationsUsed;
  }
}
