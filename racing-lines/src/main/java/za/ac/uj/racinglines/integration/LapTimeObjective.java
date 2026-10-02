package za.ac.uj.racinglines.integration;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.pso.Objective;
import za.ac.uj.racinglines.track.Track;

/**
 * Lap time of a racing line given as one lateral offset per gate.
 *
 * <p>This is the single objective every algorithm minimises. It goes through the real
 * {@link Track#racingLine(double[])} geometry and the validated {@link LapTimeSimulator}.
 */
public final class LapTimeObjective implements Objective {
  private final Track track;
  private final LapTimeSimulator simulator;
  private final double halfWidth;
  private final AtomicLong evaluations = new AtomicLong();

  public LapTimeObjective(Track track, LapTimeSimulator simulator, double trackWidth) {
    if (trackWidth <= 0) {
      throw new IllegalArgumentException("Track width must be positive, got " + trackWidth);
    }
    this.track = track;
    this.simulator = simulator;
    this.halfWidth = trackWidth / 2.0;
  }

  /** Counted evaluation: every call uses one unit of the budget. */
  @Override
  public double evaluate(double[] offsets) {
    if (offsets.length != track.gateCount()) {
      throw new IllegalArgumentException(
          "Expected " + track.gateCount() + " offsets, got " + offsets.length);
    }
    evaluations.incrementAndGet();
    return lapTimeOf(offsets);
  }

  /** Centreline lap time (all offsets zero). Not counted against the budget. */
  public double baseline() {
    return lapTimeOf(new double[track.gateCount()]);
  }

  /**
   * Recomputes a lap time without counting it. Used only to verify that the lap an optimiser
   * reports really is the lap of the line it returned.
   */
  public double lapTimeUncounted(double[] offsets) {
    return lapTimeOf(offsets);
  }

  public long evaluationsUsed() {
    return evaluations.get();
  }

  public double halfWidth() {
    return halfWidth;
  }

  private double lapTimeOf(double[] offsets) {
    double t = simulator.lapTime(track.racingLine(offsets));
    if (!Double.isFinite(t) || t <= 0) {
      throw new IllegalStateException(
          "Invalid lap time " + t + " for line " + Arrays.toString(offsets));
    }
    return t;
  }

  @Override
  public int getDimension() {
    return track.gateCount();
  }

  @Override
  public double[] getLowerBounds() {
    double[] b = new double[track.gateCount()];
    Arrays.fill(b, -halfWidth);
    return b;
  }

  @Override
  public double[] getUpperBounds() {
    double[] b = new double[track.gateCount()];
    Arrays.fill(b, halfWidth);
    return b;
  }
}