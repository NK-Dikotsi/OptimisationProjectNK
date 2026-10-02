package za.ac.uj.racinglines.integration;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.pso.Objective;
import za.ac.uj.racinglines.track.Track;

/**
 * Lap time of a racing line, the single objective every algorithm minimises.
 *
 * <p>The decision vector holds K control-point offsets, evenly spaced around the lap. They are
 * expanded to one offset per gate with a periodic Catmull-Rom spline, so every candidate line is
 * smooth. With K equal to the gate count the expansion is the identity (one offset per gate).
 * Lap time goes through the real {@link Track#racingLine(double[])} geometry and the validated
 * {@link LapTimeSimulator}.
 */
public final class LapTimeObjective implements Objective {
  private final Track track;
  private final LapTimeSimulator simulator;
  private final double halfWidth;
  private final int controlPoints;
  private final AtomicLong evaluations = new AtomicLong();

  /** One offset per gate (no spline). */
  public LapTimeObjective(Track track, LapTimeSimulator simulator, double trackWidth) {
    this(track, simulator, trackWidth, track.gateCount());
  }

  /** K control points expanded to all gates with a periodic spline. */
  public LapTimeObjective(Track track, LapTimeSimulator simulator, double trackWidth,
      int controlPoints) {
    if (trackWidth <= 0) {
      throw new IllegalArgumentException("Track width must be positive, got " + trackWidth);
    }
    if (controlPoints < 4 || controlPoints > track.gateCount()) {
      throw new IllegalArgumentException("Control points must be in [4, " + track.gateCount()
          + "], got " + controlPoints);
    }
    this.track = track;
    this.simulator = simulator;
    this.halfWidth = trackWidth / 2.0;
    this.controlPoints = controlPoints;
  }

  /** Counted evaluation: every call uses one unit of the budget. */
  @Override
  public double evaluate(double[] control) {
    checkLength(control);
    evaluations.incrementAndGet();
    return lapTimeOf(control);
  }

  /** Centreline lap time (all offsets zero). Not counted against the budget. */
  public double baseline() {
    return lapTimeOf(new double[controlPoints]);
  }

  /**
   * Recomputes a lap time without counting it. Used only to verify that the lap an optimiser
   * reports really is the lap of the line it returned.
   */
  public double lapTimeUncounted(double[] control) {
    checkLength(control);
    return lapTimeOf(control);
  }

  /**
   * Expands control-point offsets to one offset per gate. Control point k sits on gate
   * round(k * N / K). Results are clamped to the track, since a spline can overshoot slightly.
   */
  public double[] toGateOffsets(double[] control) {
    checkLength(control);
    int n = track.gateCount();
    int k = controlPoints;
    if (k == n) {
      return control.clone();
    }
    double[] gates = new double[n];
    for (int g = 0; g < n; g++) {
      double u = (double) g * k / n;
      int i = (int) Math.floor(u);
      double t = u - i;
      double p0 = control[Math.floorMod(i - 1, k)];
      double p1 = control[i % k];
      double p2 = control[(i + 1) % k];
      double p3 = control[(i + 2) % k];
      double v = 0.5 * (2 * p1
          + (-p0 + p2) * t
          + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
          + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
      gates[g] = Math.max(-halfWidth, Math.min(halfWidth, v));
    }
    return gates;
  }

  public long evaluationsUsed() {
    return evaluations.get();
  }

  public double halfWidth() {
    return halfWidth;
  }

  private void checkLength(double[] control) {
    if (control.length != controlPoints) {
      throw new IllegalArgumentException(
          "Expected " + controlPoints + " values, got " + control.length);
    }
  }

  private double lapTimeOf(double[] control) {
    double t = simulator.lapTime(track.racingLine(toGateOffsets(control)));
    if (!Double.isFinite(t) || t <= 0) {
      throw new IllegalStateException(
          "Invalid lap time " + t + " for line " + Arrays.toString(control));
    }
    return t;
  }

  @Override
  public int getDimension() {
    return controlPoints;
  }

  @Override
  public double[] getLowerBounds() {
    double[] b = new double[controlPoints];
    Arrays.fill(b, -halfWidth);
    return b;
  }

  @Override
  public double[] getUpperBounds() {
    double[] b = new double[controlPoints];
    Arrays.fill(b, halfWidth);
    return b;
  }
}
