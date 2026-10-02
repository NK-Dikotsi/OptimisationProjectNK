package za.ac.uj.racinglines.integration;

import java.util.List;

/**
 * The real outcome of one optimisation run. Every field is measured, never assumed.
 *
 * @param bestOffsets best line found, one lateral offset per gate (ACO lines already converted)
 * @param history best-so-far lap after each iteration, against evaluations used
 */
public record RunOutcome(
    String algorithm,
    String track,
    double bestLap,
    double baselineLap,
    double[] bestOffsets,
    long evaluationsUsed,
    long evalsTo1Pct,
    List<Point> history) {

  /** One convergence point: evaluations used so far and the best lap found so far. */
  public record Point(long evaluations, double bestLap) {}

  public double improvementPct() {
    return (baselineLap - bestLap) / baselineLap * 100.0;
  }

  /** First evaluation count at which the best-so-far lap is within 1% of the final best. */
  static long evalsTo1Pct(List<Point> history, double finalBest) {
    double threshold = finalBest * 1.01;
    for (Point p : history) {
      if (p.bestLap() <= threshold) {
        return p.evaluations();
      }
    }
    throw new IllegalStateException("Convergence history never reached the final best lap");
  }
}
