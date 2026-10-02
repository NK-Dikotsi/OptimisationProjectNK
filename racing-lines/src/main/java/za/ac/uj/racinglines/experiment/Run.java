package za.ac.uj.racinglines.experiment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Run {
  private final String runId;
  private final String algorithm;
  private final String track;
  private final int seed;
  private final long budget;
  private final Path runFolder;
  private final Instant createdAt;

  private Double bestLap;
  private Double baselineLap;
  private Long evaluationsUsed;
  private Long duration;
  private Long evalsTo1Pct;
  private Instant completedAt;
  private String status; // "pending", "running", "completed", "failed"
  private String failureReason;

  public String getRunId() {
    return runId;
  }

  public boolean isCompleted() {
    return "completed".equals(status);
  }

  public boolean isFailed() {
    return "failed".equals(status);
  }

  public double getImprovementPct() {
    if (baselineLap == null || baselineLap == 0) {
      return 0.0;
    }
    return ((baselineLap - bestLap) / baselineLap) * 100.0;
  }
}
