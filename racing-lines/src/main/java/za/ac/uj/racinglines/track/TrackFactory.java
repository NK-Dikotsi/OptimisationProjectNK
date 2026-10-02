package za.ac.uj.racinglines.track;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for the experiment tracks.
 *
 * <p>Every track is a <b>closed circuit</b> built from straights and circular arcs by integrating
 * the heading, so the centreline is continuous, smooth and returns exactly to its start. This
 * matters because {@link Track#fromCentreline} treats the centreline as a closed loop: an open
 * path would get an artificial closing segment from the last point back to the first.
 */
public class TrackFactory {

  /** Gates used for the experiment tracks (about 4-5 m spacing). */
  public static final int STANDARD_GATES = 100;

  /** Centreline sampling step before resampling to gates, in metres. */
  private static final double STEP = 0.25;

  private static final TrackFactory factory = new TrackFactory();

  /** Oval: two 150 m straights joined by two 180-degree turns of radius 30 m (488 m). */
  public static Track ovalStandard() {
    return factory.oval("oval", 150.0, 0.0, 30.0, 12.0, STANDARD_GATES);
  }

  /** Double hairpin: two 150 m straights joined by two tight 180-degree turns of radius 12 m (375 m). */
  public static Track hairpinStandard() {
    return factory.hairpin("hairpin", 12.0, 150.0, 12.0, STANDARD_GATES);
  }

  /**
   * Oval with an S-chicane in the bottom straight: 45 degrees left, 90 right, 45 left, all
   * radius 20 m, giving an 11.7 m sideways kink on a 12 m wide track (495 m).
   */
  public static Track chicaneStandard() {
    return factory.chicane("chicane", 20.0, 150.0, 12.0, STANDARD_GATES);
  }

  /** Circle of the given radius (closed by construction). */
  public Track circle(String name, double radius, double trackWidth, int nGates) {
    double[][] points = new double[nGates][2];
    for (int i = 0; i < nGates; i++) {
      double theta = 2.0 * Math.PI * i / nGates;
      points[i][0] = radius * Math.cos(theta);
      points[i][1] = radius * Math.sin(theta);
    }
    return Track.fromCentreline(name, points, trackWidth);
  }

  /**
   * Oval: straight, 180-degree turn, straight, 180-degree turn.
   *
   * @param straightLength length of each straight in metres
   * @param unused kept so existing callers (TrackLoader) still compile; the separation between the
   *     straights is always 2 x radius
   * @param radius radius of both end turns in metres
   */
  public Track oval(String name, double straightLength, double unused, double radius,
      double trackWidth, int nGates) {
    return resampled(name, new Builder()
        .straight(straightLength).arc(radius, 180)
        .straight(straightLength).arc(radius, 180), trackWidth, nGates);
  }

  /** Double hairpin: an oval whose two ends are tight hairpin turns of the given radius. */
  public Track hairpin(String name, double radius, double straightLength, double trackWidth,
      int nGates) {
    return resampled(name, new Builder()
        .straight(straightLength).arc(radius, 180)
        .straight(straightLength).arc(radius, 180), trackWidth, nGates);
  }

  /**
   * Oval (end radius 30 m) with a symmetric S-chicane in the bottom straight.
   *
   * @param chicaneRadius radius of the three chicane arcs (45 left, 90 right, 45 left)
   * @param straightLength length of the top straight; the bottom straight plus chicane spans the
   *     same distance so the circuit closes
   */
  public Track chicane(String name, double chicaneRadius, double straightLength,
      double trackWidth, int nGates) {
    double endRadius = 30.0;
    double chicaneSpan = 4.0 * chicaneRadius * Math.sin(Math.toRadians(45));
    double lead = (straightLength - chicaneSpan) / 2.0;
    if (lead <= 0) {
      throw new IllegalArgumentException("Straight too short for a chicane of radius "
          + chicaneRadius);
    }
    return resampled(name, new Builder()
        .straight(lead).arc(chicaneRadius, 45).arc(chicaneRadius, -90).arc(chicaneRadius, 45)
        .straight(lead).arc(endRadius, 180)
        .straight(straightLength).arc(endRadius, 180), trackWidth, nGates);
  }

  /** Samples the builder finely, then lets Track resample to exactly nGates even gates. */
  private static Track resampled(String name, Builder b, double trackWidth, int nGates) {
    double[][] fine = b.closedPoints();
    double[][] gates = resampleClosed(fine, nGates);
    return Track.fromCentreline(name, gates, trackWidth);
  }

  /** Evenly spaced points along a closed polyline. */
  static double[][] resampleClosed(double[][] pts, int n) {
    int m = pts.length;
    double[] cum = new double[m + 1];
    for (int i = 0; i < m; i++) {
      double[] a = pts[i];
      double[] c = pts[(i + 1) % m];
      cum[i + 1] = cum[i] + Math.hypot(c[0] - a[0], c[1] - a[1]);
    }
    double total = cum[m];
    double[][] out = new double[n][2];
    int seg = 0;
    for (int k = 0; k < n; k++) {
      double s = total * k / n;
      while (cum[seg + 1] < s) {
        seg++;
      }
      double t = (s - cum[seg]) / (cum[seg + 1] - cum[seg]);
      double[] a = pts[seg];
      double[] c = pts[(seg + 1) % m];
      out[k][0] = a[0] + t * (c[0] - a[0]);
      out[k][1] = a[1] + t * (c[1] - a[1]);
    }
    return out;
  }

  /** Builds a centreline by integrating heading through straights and arcs. */
  static final class Builder {
    private final List<double[]> points = new ArrayList<>();
    private double x;
    private double y;
    private double heading;

    Builder() {
      points.add(new double[] {0, 0});
    }

    Builder straight(double length) {
      int n = Math.max(1, (int) Math.round(length / STEP));
      for (int i = 0; i < n; i++) {
        x += length / n * Math.cos(heading);
        y += length / n * Math.sin(heading);
        points.add(new double[] {x, y});
      }
      return this;
    }

    /** Arc of the given radius; positive degrees turn left, negative turn right. */
    Builder arc(double radius, double degrees) {
      double angle = Math.toRadians(degrees);
      double length = radius * Math.abs(angle);
      int n = Math.max(1, (int) Math.round(length / STEP));
      double dTheta = angle / n;
      double chord = 2.0 * radius * Math.sin(Math.abs(dTheta) / 2.0);
      for (int i = 0; i < n; i++) {
        double mid = heading + dTheta / 2.0;
        x += chord * Math.cos(mid);
        y += chord * Math.sin(mid);
        heading += dTheta;
        points.add(new double[] {x, y});
      }
      return this;
    }

    /** Points of the closed loop (the duplicate end point is dropped). Throws if not closed. */
    double[][] closedPoints() {
      double[] last = points.get(points.size() - 1);
      double gap = Math.hypot(last[0], last[1]);
      if (gap > 0.01) {
        throw new IllegalStateException("Track does not close: end is " + gap + " m from start");
      }
      return points.subList(0, points.size() - 1).toArray(new double[0][]);
    }
  }
}
