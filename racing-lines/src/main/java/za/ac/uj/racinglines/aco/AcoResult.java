package za.ac.uj.racinglines.aco;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AcoResult {
  private final int[] bestLine;
  private final double bestLap;
  private final long evaluationsUsed;
  private final long durationMs;
  private final String terminationReason;
  private final List<IterationSnapshot> history;

  @Getter
  @RequiredArgsConstructor
  public static class IterationSnapshot {
    private final int iteration;
    private final long evaluations;
    private final double bestLap;
    private final double iterationBestLap;
    private final double meanLap;
    private final double pheromoneEntropy;
    private final double tauMin;
    private final double tauMax;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int[] bestLine;
    private double bestLap;
    private long evaluationsUsed;
    private long durationMs;
    private String terminationReason;
    private List<IterationSnapshot> history = new ArrayList<>();

    public Builder bestLine(int[] l) { this.bestLine = l; return this; }
    public Builder bestLap(double f) { this.bestLap = f; return this; }
    public Builder evaluationsUsed(long e) { this.evaluationsUsed = e; return this; }
    public Builder durationMs(long d) { this.durationMs = d; return this; }
    public Builder terminationReason(String r) { this.terminationReason = r; return this; }
    public Builder history(List<IterationSnapshot> h) { this.history = h; return this; }

    public AcoResult build() {
      return new AcoResult(bestLine, bestLap, evaluationsUsed, durationMs, terminationReason, history);
    }
  }
}
