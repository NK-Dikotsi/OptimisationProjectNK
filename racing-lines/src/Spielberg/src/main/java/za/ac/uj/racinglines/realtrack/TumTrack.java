package za.ac.uj.racinglines.realtrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import za.ac.uj.racinglines.track.Track;

/**
 * A real circuit loaded from the TUM racetrack database
 * (columns x_m, y_m, w_tr_right_m, w_tr_left_m; lines starting with # are comments).
 *
 * <p>The project's Track model has one width for the whole lap, so the narrowest point sets
 * the width: trackWidth = 2 x the smallest of all left and right half-widths. This keeps every
 * line legal everywhere, at the cost of not using the extra width where the real track is wider.
 */
public final class TumTrack {
  private final String name;
  private final double[][] centreline;
  private final double minHalfWidth;
  private final double medianHalfWidth;

  private TumTrack(String name, double[][] centreline, double minHalfWidth,
      double medianHalfWidth) {
    this.name = name;
    this.centreline = centreline;
    this.minHalfWidth = minHalfWidth;
    this.medianHalfWidth = medianHalfWidth;
  }

  public static TumTrack load(String name, Path csv) throws IOException {
    List<double[]> pts = new ArrayList<>();
    List<Double> halves = new ArrayList<>();
    for (String raw : Files.readAllLines(csv)) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String[] f = line.split(",");
      if (f.length < 4) {
        throw new IOException("Expected 4 columns in " + csv + ", got: " + line);
      }
      pts.add(new double[] {Double.parseDouble(f[0]), Double.parseDouble(f[1])});
      halves.add(Math.min(Double.parseDouble(f[2]), Double.parseDouble(f[3])));
    }
    if (pts.size() < 10) {
      throw new IOException("Too few points in " + csv + ": " + pts.size());
    }
    double min = halves.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    double[] sorted = halves.stream().mapToDouble(Double::doubleValue).sorted().toArray();
    return new TumTrack(name, pts.toArray(new double[0][]), min, sorted[sorted.length / 2]);
  }

  /** Reads an x_m, y_m polyline (for example TUM's reference race line). */
  public static double[][] loadPolyline(Path csv) throws IOException {
    List<double[]> pts = new ArrayList<>();
    for (String raw : Files.readAllLines(csv)) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String[] f = line.split(",");
      pts.add(new double[] {Double.parseDouble(f[0]), Double.parseDouble(f[1])});
    }
    return pts.toArray(new double[0][]);
  }

  /** Uniform track width used for the model: twice the narrowest half-width. */
  public double trackWidth() {
    return 2.0 * minHalfWidth;
  }

  public Track toTrack() {
    return Track.fromCentreline(name, centreline, trackWidth());
  }

  public double lengthMetres() {
    double sum = 0;
    int n = centreline.length;
    for (int i = 0; i < n; i++) {
      double[] a = centreline[i];
      double[] b = centreline[(i + 1) % n];
      sum += Math.hypot(b[0] - a[0], b[1] - a[1]);
    }
    return sum;
  }

  public String name() {
    return name;
  }

  public int points() {
    return centreline.length;
  }

  public double minHalfWidth() {
    return minHalfWidth;
  }

  public double medianHalfWidth() {
    return medianHalfWidth;
  }
}
