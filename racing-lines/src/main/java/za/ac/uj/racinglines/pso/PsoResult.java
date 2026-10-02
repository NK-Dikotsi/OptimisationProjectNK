package za.ac.uj.racinglines.pso;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class PsoResult {
  private final double[] bestVector;
  private final double bestFitness;
  private final long evaluationsUsed;
  private final long durationMs;
  private final String terminationReason;
  private final List<IterationSnapshot> history;

  @Getter
  @RequiredArgsConstructor
  public static class IterationSnapshot {
    private final int iteration;
    private final long evaluations;
    private final double gbestFitness;
    private final double meanFitness;
    private final double diversity;
    private final double inertiaWeight;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private double[] bestVector;
    private double bestFitness;
    private long evaluationsUsed;
    private long durationMs;
    private String terminationReason;
    private List<IterationSnapshot> history = new ArrayList<>();

    public Builder bestVector(double[] v) { this.bestVector = v; return this; }
    public Builder bestFitness(double f) { this.bestFitness = f; return this; }
    public Builder evaluationsUsed(long e) { this.evaluationsUsed = e; return this; }
    public Builder durationMs(long d) { this.durationMs = d; return this; }
    public Builder terminationReason(String r) { this.terminationReason = r; return this; }
    public Builder history(List<IterationSnapshot> h) { this.history = h; return this; }

    public PsoResult build() {
      return new PsoResult(bestVector, bestFitness, evaluationsUsed, durationMs, terminationReason, history);
    }
  }
}
