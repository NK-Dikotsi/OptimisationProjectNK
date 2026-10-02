package za.ac.uj.racinglines.track;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.logstash.logback.argument.StructuredArguments;

@Getter
public class Track {
  private static final Logger logger = LoggerFactory.getLogger(Track.class);
  private static final double ABSOLUTE_TOLERANCE = 1e-9;

  private final String name;
  private final double[][] centrelinePoints;
  private final double[][] normals;
  private final double width;
  private final int nGates;
  private final double centrelineLength;

  private Track(String name, double[][] centrelinePoints, double[][] normals, double width) {
    this.name = name;
    this.centrelinePoints = centrelinePoints;
    this.normals = normals;
    this.width = width;
    this.nGates = centrelinePoints.length;
    this.centrelineLength = computeCentrelineLength();

    logger.info("track_loaded",
        StructuredArguments.kv("name", name),
        StructuredArguments.kv("n_gates", nGates),
        StructuredArguments.kv("length_m", centrelineLength),
        StructuredArguments.kv("width_m", width),
        StructuredArguments.kv("min_radius_m", computeMinRadius()));
  }

  /**
   * Create a Track from a centreline and uniform track width.
   * Resamples the centreline to N evenly-spaced gates and computes normals.
   */
  public static Track fromCentreline(String name, double[][] centreline, double width) {
    // Resample centreline to even spacing
    int targetGates = centreline.length;
    double[][] resampled = resampleToEvenSpacing(centreline, targetGates);

    // Compute normals at each gate
    double[][] normals = computeNormals(resampled);

    logger.debug("track_resampled",
        StructuredArguments.kv("original_points", centreline.length),
        StructuredArguments.kv("new_n", targetGates),
        StructuredArguments.kv("mean_spacing_m", computeMeanSpacing(resampled)));

    return new Track(name, resampled, normals, width);
  }

  /**
   * Return the racing line as [x, y] points given an offset vector.
   * offset[i] is the perpendicular offset at gate i (positive = towards normal, negative = away).
   * Offsets outside ±width/2 are clamped with a warning.
   */
  public double[][] racingLine(double[] offsets) {
    if (offsets.length != nGates) {
      throw new IllegalArgumentException(
          "Offset vector length " + offsets.length + " does not match gate count " + nGates);
    }

    double[][] line = new double[nGates][2];
    double halfWidth = width / 2.0;
    int clamped = 0;
    double maxViolation = 0;

    for (int i = 0; i < nGates; i++) {
      double offset = offsets[i];

      // Clamp offset to ±width/2
      if (offset > halfWidth) {
        maxViolation = Math.max(maxViolation, offset - halfWidth);
        offset = halfWidth;
        clamped++;
      } else if (offset < -halfWidth) {
        maxViolation = Math.max(maxViolation, -halfWidth - offset);
        offset = -halfWidth;
        clamped++;
      }

      // Racing line point = centreline + offset * normal
      line[i][0] = centrelinePoints[i][0] + offset * normals[i][0];
      line[i][1] = centrelinePoints[i][1] + offset * normals[i][1];
    }

    if (clamped > 0) {
      logger.warn("offsets_clamped",
          StructuredArguments.kv("count", clamped),
          StructuredArguments.kv("largest_violation_m", maxViolation));
    }

    return line;
  }

  // Convenience accessors
  public int gateCount() { return nGates; }

  public double[] gateSpacings() {
    double[] spacings = new double[nGates];
    for (int i = 0; i < nGates; i++) {
      int next = (i + 1) % nGates;
      double dx = centrelinePoints[next][0] - centrelinePoints[i][0];
      double dy = centrelinePoints[next][1] - centrelinePoints[i][1];
      spacings[i] = Math.sqrt(dx * dx + dy * dy);
    }
    return spacings;
  }

  // Private helpers

  private static double[][] resampleToEvenSpacing(double[][] original, int targetN) {
    // Compute total arc length
    double totalLength = 0;
    double[] segmentLengths = new double[original.length];

    for (int i = 0; i < original.length; i++) {
      int next = (i + 1) % original.length;
      double dx = original[next][0] - original[i][0];
      double dy = original[next][1] - original[i][1];
      segmentLengths[i] = Math.sqrt(dx * dx + dy * dy);
      totalLength += segmentLengths[i];
    }

    // Create resampled points at even arc-length intervals
    double[][] resampled = new double[targetN][2];
    double arcLengthPerSample = totalLength / targetN;

    double currentArcLength = 0;
    int sourceIdx = 0;

    for (int i = 0; i < targetN; i++) {
      double targetArcLength = i * arcLengthPerSample;

      // Advance sourceIdx to bracket the target arc length
      while (sourceIdx < original.length && currentArcLength + segmentLengths[sourceIdx] < targetArcLength) {
        currentArcLength += segmentLengths[sourceIdx];
        sourceIdx++;
      }

      int next = (sourceIdx + 1) % original.length;
      double segLen = segmentLengths[sourceIdx];

      if (segLen < ABSOLUTE_TOLERANCE) {
        // Degenerate segment, just use source point
        resampled[i][0] = original[sourceIdx][0];
        resampled[i][1] = original[sourceIdx][1];
      } else {
        // Interpolate within the segment
        double t = (targetArcLength - currentArcLength) / segLen;
        t = Math.max(0, Math.min(1, t)); // Clamp to [0, 1]

        resampled[i][0] = original[sourceIdx][0] + t * (original[next][0] - original[sourceIdx][0]);
        resampled[i][1] = original[sourceIdx][1] + t * (original[next][1] - original[sourceIdx][1]);
      }
    }

    return resampled;
  }

  private static double[][] computeNormals(double[][] centreline) {
    int n = centreline.length;
    double[][] normals = new double[n][2];

    for (int i = 0; i < n; i++) {
      int prev = (i - 1 + n) % n;
      int next = (i + 1) % n;

      // Tangent: average of incoming and outgoing directions
      double inX = centreline[i][0] - centreline[prev][0];
      double inY = centreline[i][1] - centreline[prev][1];
      double outX = centreline[next][0] - centreline[i][0];
      double outY = centreline[next][1] - centreline[i][1];

      double tangentX = inX + outX;
      double tangentY = inY + outY;

      double tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY);
      if (tangentLen < ABSOLUTE_TOLERANCE) {
        // Degenerate case, use perpendicular to outgoing
        tangentX = outX;
        tangentY = outY;
        tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY);
      }

      if (tangentLen < ABSOLUTE_TOLERANCE) {
        // Still degenerate, use default
        normals[i][0] = 1.0;
        normals[i][1] = 0.0;
      } else {
        // Normal is tangent rotated 90° counter-clockwise
        normals[i][0] = -tangentY / tangentLen;
        normals[i][1] = tangentX / tangentLen;
      }
    }

    // Ensure normals point consistently (all "left" from the direction of travel)
    ensureConsistentNormals(centreline, normals);

    return normals;
  }

  private static void ensureConsistentNormals(double[][] centreline, double[][] normals) {
    // Average the centroid
    double centroidX = 0, centroidY = 0;
    for (int i = 0; i < centreline.length; i++) {
      centroidX += centreline[i][0];
      centroidY += centreline[i][1];
    }
    centroidX /= centreline.length;
    centroidY /= centreline.length;

    // At the first gate, ensure the normal points outward from the centroid
    double toOuterX = centreline[0][0] - centroidX;
    double toOuterY = centreline[0][1] - centroidY;

    if (normals[0][0] * toOuterX + normals[0][1] * toOuterY < 0) {
      // Normal points inward, flip all normals
      for (int i = 0; i < normals.length; i++) {
        normals[i][0] = -normals[i][0];
        normals[i][1] = -normals[i][1];
      }
    }
  }

  private static double computeMeanSpacing(double[][] points) {
    double total = 0;
    for (int i = 0; i < points.length; i++) {
      int next = (i + 1) % points.length;
      double dx = points[next][0] - points[i][0];
      double dy = points[next][1] - points[i][1];
      total += Math.sqrt(dx * dx + dy * dy);
    }
    return total / points.length;
  }

  private double computeCentrelineLength() {
    double length = 0;
    for (int i = 0; i < nGates; i++) {
      int next = (i + 1) % nGates;
      double dx = centrelinePoints[next][0] - centrelinePoints[i][0];
      double dy = centrelinePoints[next][1] - centrelinePoints[i][1];
      length += Math.sqrt(dx * dx + dy * dy);
    }
    return length;
  }

  private double computeMinRadius() {
    double minRadius = Double.MAX_VALUE;

    for (int i = 1; i < nGates - 1; i++) {
      double x1 = centrelinePoints[i - 1][0];
      double y1 = centrelinePoints[i - 1][1];
      double x2 = centrelinePoints[i][0];
      double y2 = centrelinePoints[i][1];
      double x3 = centrelinePoints[i + 1][0];
      double y3 = centrelinePoints[i + 1][1];

      double radius = computeCircleRadius(x1, y1, x2, y2, x3, y3);
      if (radius > 0) {
        minRadius = Math.min(minRadius, radius);
      }
    }

    return minRadius == Double.MAX_VALUE ? 0 : minRadius;
  }

  private double computeCircleRadius(double x1, double y1, double x2, double y2, double x3, double y3) {
    double d = 2.0 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
    if (Math.abs(d) < ABSOLUTE_TOLERANCE) return -1;

    double a = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    double b = Math.sqrt((x2 - x3) * (x2 - x3) + (y2 - y3) * (y2 - y3));
    double c = Math.sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1));

    double area = Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0);
    return (a * b * c) / (4.0 * area);
  }
}
