package za.ac.uj.racinglines.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ResultRow {
  private final String runId;
  private final String algorithm;
  private final String track;
  private final int seed;
  private final double bestLap;
  private final double baselineLap;
  private final double improvementPct;
  private final long evalsUsed;
}
