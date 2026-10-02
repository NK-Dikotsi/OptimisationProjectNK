package za.ac.uj.racinglines.track;

import lombok.NoArgsConstructor;

/**
 * Factory for creating common track geometries.
 */
@NoArgsConstructor
public class TrackFactory {

  private static final TrackFactory factory = new TrackFactory();

  /** Standard oval track used in experiments. */
  public static Track ovalStandard() {
    return factory.oval("oval", 100.0, 30.0, 20.0, 12.0, 20);
  }

  /** Standard hairpin track used in experiments. */
  public static Track hairpinStandard() {
    return factory.hairpin("hairpin", 30.0, 80.0, 12.0, 25);
  }

  /** Standard chicane track used in experiments. */
  public static Track chicaneStandard() {
    return factory.chicane("chicane", 15.0, 60.0, 12.0, 30);
  }

  /**
   * Create a circular track with given radius.
   */
  public Track circle(String name, double radius, double trackWidth, int nGates) {
    double[][] centreline = generateCircle(radius, nGates);
    return Track.fromCentreline(name, centreline, trackWidth);
  }

  /**
   * Create an oval track with straight sections and semicircular ends.
   */
  public Track oval(String name, double length, double width, double radius,
                    double trackWidth, int nGates) {
    double[][] centreline = generateOval(length, width, radius, nGates);
    return Track.fromCentreline(name, centreline, trackWidth);
  }

  /**
   * Create a hairpin track (U-turn).
   */
  public Track hairpin(String name, double radius, double straightLength,
                       double trackWidth, int nGates) {
    double[][] centreline = generateHairpin(radius, straightLength, nGates);
    return Track.fromCentreline(name, centreline, trackWidth);
  }

  /**
   * Create a chicane track (S-curve).
   */
  public Track chicane(String name, double amplitude, double wavelength,
                       double trackWidth, int nGates) {
    double[][] centreline = generateChicane(amplitude, wavelength, nGates);
    return Track.fromCentreline(name, centreline, trackWidth);
  }

  // Generation methods

  private double[][] generateCircle(double radius, int nGates) {
    double[][] points = new double[nGates][2];
    for (int i = 0; i < nGates; i++) {
      double theta = 2.0 * Math.PI * i / nGates;
      points[i][0] = radius * Math.cos(theta);
      points[i][1] = radius * Math.sin(theta);
    }
    return points;
  }

  private double[][] generateOval(double length, double width, double radius, int nGates) {
    double[][] points = new double[nGates][2];
    double perimeter = 2.0 * length + 2.0 * Math.PI * radius;

    for (int i = 0; i < nGates; i++) {
      double s = (perimeter * i) / nGates;

      if (s < length) {
        // First straight (bottom)
        points[i][0] = s;
        points[i][1] = 0;
      } else if (s < length + Math.PI * radius) {
        // Right semicircle
        double theta = Math.PI * (s - length) / (Math.PI * radius);
        points[i][0] = length + radius * Math.sin(theta);
        points[i][1] = radius * (1.0 - Math.cos(theta));
      } else if (s < 2.0 * length + Math.PI * radius) {
        // Second straight (top, reversed)
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

  private double[][] generateHairpin(double radius, double straightLength, int nGates) {
    double[][] points = new double[nGates][2];
    double totalLength = straightLength + Math.PI * radius + straightLength;

    for (int i = 0; i < nGates; i++) {
      double s = (totalLength * i) / nGates;

      if (s < straightLength) {
        // Approach
        points[i][0] = s;
        points[i][1] = 0;
      } else if (s < straightLength + Math.PI * radius) {
        // U-turn
        double theta = Math.PI * (s - straightLength) / (Math.PI * radius);
        points[i][0] = straightLength + radius * Math.sin(theta);
        points[i][1] = radius * (1.0 - Math.cos(theta));
      } else {
        // Exit
        points[i][0] = straightLength - (s - straightLength - Math.PI * radius);
        points[i][1] = 2.0 * radius;
      }
    }
    return points;
  }

  private double[][] generateChicane(double amplitude, double wavelength, int nGates) {
    double[][] points = new double[nGates][2];
    double totalLength = 3.0 * wavelength;

    for (int i = 0; i < nGates; i++) {
      double s = (totalLength * i) / nGates;
      points[i][0] = s;
      points[i][1] = amplitude * Math.sin(2.0 * Math.PI * s / wavelength);
    }
    return points;
  }
}
