package za.ac.uj.racinglines.fixtures;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public class Tracks {

  private static final double CIRCLE_RADIUS = 50.0;
  private static final double OVAL_LENGTH = 200.0;
  private static final double OVAL_WIDTH = 100.0;
  private static final double TRACK_WIDTH = 10.0;
  private static final double HAIRPIN_RADIUS = 10.0;
  private static final double CHICANE_WIDTH_DELTA = 5.0;

  // Evaluation budgets
  public static final int TINY_BUDGET = 200;
  public static final int SMALL_BUDGET = 1000;
  public static final int MEDIUM_BUDGET = 5000;
  public static final int LARGE_BUDGET = 10000;

  // RNG factory
  public static RandomGenerator rng(long seed) {
    return new SplittableRandom(seed);
  }

  // Circle track: simple geometry for testing
  // Centreline: circle of radius R
  // Width: uniform w
  public static class CircleTrack {
    public final double radius;
    public final double width;
    public final int nGates;

    public CircleTrack(double radius, double width, int nGates) {
      this.radius = radius;
      this.width = width;
      this.nGates = nGates;
    }

    public double[][] centreline() {
      double[][] points = new double[nGates][2];
      for (int i = 0; i < nGates; i++) {
        double theta = 2.0 * Math.PI * i / nGates;
        points[i][0] = radius * Math.cos(theta);
        points[i][1] = radius * Math.sin(theta);
      }
      return points;
    }

    public double expectedCentrelineLength() {
      return 2.0 * Math.PI * radius;
    }
  }

  // Oval track: two straight sections + semicircles at ends
  // Tests transitions between straight and curved sections
  public static class OvalTrack {
    public final double length;   // total length of straight sections
    public final double width;    // total width (for the straight parts)
    public final double radius;   // radius of the semicircles at ends
    public final double trackWidth;
    public final int nGates;

    public OvalTrack(double length, double width, double radius, double trackWidth, int nGates) {
      this.length = length;
      this.width = width;
      this.radius = radius;
      this.trackWidth = trackWidth;
      this.nGates = nGates;
    }

    public double[][] centreline() {
      // Simple oval: two horizontal lines + two semicircles
      double[][] points = new double[nGates][2];
      double perimeter = 2.0 * length + 2.0 * Math.PI * radius;

      for (int i = 0; i < nGates; i++) {
        double s = (perimeter * i) / nGates; // arc length

        if (s < length) {
          // First straight section (bottom)
          points[i][0] = s;
          points[i][1] = 0;
        } else if (s < length + Math.PI * radius) {
          // Right semicircle
          double theta = Math.PI * (s - length) / (Math.PI * radius);
          points[i][0] = length + radius * Math.sin(theta);
          points[i][1] = radius * (1.0 - Math.cos(theta));
        } else if (s < 2.0 * length + Math.PI * radius) {
          // Second straight section (top, reversed)
          points[i][0] = length - (s - length - Math.PI * radius);
          points[i][1] = 2.0 * radius;
        } else {
          // Left semicircle
          double theta = Math.PI * (s - 2.0 * length - Math.PI * radius) / (Math.PI * radius);
          points[i][0] = radius * (1.0 - Math.sin(theta));
          points[i][1] = radius * Math.cos(theta);
        }
      }
      return points;
    }
  }

  // Hairpin track: sharp turn with tight radius
  // Tests minimum radius detection
  public static class HairpinTrack {
    public final double radius;     // minimum radius at the turn
    public final double straightLength;
    public final double trackWidth;
    public final int nGates;

    public HairpinTrack(double radius, double straightLength, double trackWidth, int nGates) {
      this.radius = radius;
      this.straightLength = straightLength;
      this.trackWidth = trackWidth;
      this.nGates = nGates;
    }

    public double[][] centreline() {
      double[][] points = new double[nGates][2];
      double totalLength = straightLength + Math.PI * radius + straightLength;

      for (int i = 0; i < nGates; i++) {
        double s = (totalLength * i) / nGates;

        if (s < straightLength) {
          // Approach straight
          points[i][0] = s;
          points[i][1] = 0;
        } else if (s < straightLength + Math.PI * radius) {
          // U-turn (semicircle)
          double theta = Math.PI * (s - straightLength) / (Math.PI * radius);
          points[i][0] = straightLength + radius * Math.sin(theta);
          points[i][1] = radius * (1.0 - Math.cos(theta));
        } else {
          // Exit straight
          points[i][0] = straightLength - (s - straightLength - Math.PI * radius);
          points[i][1] = 2.0 * radius;
        }
      }
      return points;
    }
  }

  // Chicane track: S-shaped curves
  // Tests multiple direction changes
  public static class ChicaneTrack {
    public final double amplitude; // lateral deviation
    public final double wavelength;
    public final double trackWidth;
    public final int nGates;

    public ChicaneTrack(double amplitude, double wavelength, double trackWidth, int nGates) {
      this.amplitude = amplitude;
      this.wavelength = wavelength;
      this.trackWidth = trackWidth;
      this.nGates = nGates;
    }

    public double[][] centreline() {
      double[][] points = new double[nGates][2];
      double totalLength = 3.0 * wavelength;

      for (int i = 0; i < nGates; i++) {
        double s = (totalLength * i) / nGates;
        points[i][0] = s;
        points[i][1] = amplitude * Math.sin(2.0 * Math.PI * s / wavelength);
      }
      return points;
    }

    public int expectedDirectionChanges() {
      return 2; // S-shape: one left turn, one right turn
    }
  }

  // Factory methods
  public static CircleTrack circle(double radius) {
    return new CircleTrack(radius, TRACK_WIDTH, 500);
  }

  public static CircleTrack circle(double radius, int nGates) {
    return new CircleTrack(radius, TRACK_WIDTH, nGates);
  }

  public static OvalTrack oval() {
    return new OvalTrack(OVAL_LENGTH, OVAL_WIDTH, 50.0, TRACK_WIDTH, 400);
  }

  public static HairpinTrack hairpin() {
    return new HairpinTrack(HAIRPIN_RADIUS, 100.0, TRACK_WIDTH, 300);
  }

  public static ChicaneTrack chicane() {
    return new ChicaneTrack(CHICANE_WIDTH_DELTA, 50.0, TRACK_WIDTH, 300);
  }
}
