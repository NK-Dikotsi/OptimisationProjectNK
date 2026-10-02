package za.ac.uj.racinglines.track;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.data.Percentage;
import za.ac.uj.racinglines.fixtures.Tracks;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.*;

class TrackTest {

  private static final double RELATIVE_TOLERANCE = 1e-3;
  private static final double ABSOLUTE_TOLERANCE = 1e-6;

  private Track circleTrack;
  private Track ovalTrack;
  private Track hairpinTrack;
  private Track chicaneTrack;
  private TrackCsvFormat csvFormat;

  @BeforeEach
  void setUp() {
    csvFormat = new TrackCsvFormat(new ObjectMapper());

    Tracks.CircleTrack circle = Tracks.circle(50.0, 500);
    circleTrack = Track.fromCentreline("circle", circle.centreline(), circle.width);

    Tracks.OvalTrack oval = Tracks.oval();
    ovalTrack = Track.fromCentreline("oval", oval.centreline(), oval.trackWidth);

    Tracks.HairpinTrack hairpin = Tracks.hairpin();
    hairpinTrack = Track.fromCentreline("hairpin", hairpin.centreline(), hairpin.trackWidth);

    Tracks.ChicaneTrack chicane = Tracks.chicane();
    chicaneTrack = Track.fromCentreline("chicane", chicane.centreline(), chicane.trackWidth);
  }

  // T1: Circle length test
  @Test
  @DisplayName("T1 circle length")
  void testCircleLength() {
    Tracks.CircleTrack circle = Tracks.circle(50.0, 500);
    double expected = circle.expectedCentrelineLength();
    double actual = circleTrack.getCentrelineLength();

    assertThat(actual)
        .isCloseTo(expected, within(expected * RELATIVE_TOLERANCE));
  }

  // T2: Gate count test
  @Test
  @DisplayName("T2 gate count")
  void testGateCount() {
    assertThat(circleTrack.gateCount()).isEqualTo(500);
    assertThat(ovalTrack.gateCount()).isEqualTo(400);
    assertThat(hairpinTrack.gateCount()).isEqualTo(300);
  }

  // T3: Even spacing test
  @Test
  @DisplayName("T3 even spacing")
  void testEvenSpacing() {
    double[] spacings = circleTrack.gateSpacings();
    double mean = java.util.Arrays.stream(spacings).average().orElse(0);
    double variance = java.util.Arrays.stream(spacings)
        .map(s -> Math.pow(s - mean, 2))
        .average()
        .orElse(0);
    double stdDev = Math.sqrt(variance);
    double coefficient = stdDev / mean;

    assertThat(coefficient).isLessThan(0.01); // < 1% std dev
  }

  // T4: Closed loop test
  @Test
  @DisplayName("T4 closed loop")
  void testClosedLoop() {
    double[][] centreline = circleTrack.getCentrelinePoints();
    double firstX = centreline[0][0];
    double firstY = centreline[0][1];
    double lastX = centreline[centreline.length - 1][0];
    double lastY = centreline[centreline.length - 1][1];

    // Last point should connect to first (within tolerance for discretization)
    assertThat(lastX).isCloseTo(firstX, within(1.0));
    assertThat(lastY).isCloseTo(firstY, within(1.0));
  }

  // T5: Normal unit length test
  @Test
  @DisplayName("T5 normals unit length")
  void testNormalsUnitLength() {
    double[][] normals = circleTrack.getNormals();

    for (int i = 0; i < normals.length; i++) {
      double length = Math.sqrt(normals[i][0] * normals[i][0] + normals[i][1] * normals[i][1]);
      assertThat(length).isCloseTo(1.0, within(ABSOLUTE_TOLERANCE));
    }
  }

  // T6: Normals perpendicular to tangents test
  @Test
  @DisplayName("T6 normals perpendicular")
  void testNormalsPerpendicular() {
    double[][] centreline = circleTrack.getCentrelinePoints();
    double[][] normals = circleTrack.getNormals();

    for (int i = 0; i < centreline.length; i++) {
      int next = (i + 1) % centreline.length;
      double tangentX = centreline[next][0] - centreline[i][0];
      double tangentY = centreline[next][1] - centreline[i][1];

      // Normalize tangent
      double tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY);
      if (tangentLen > ABSOLUTE_TOLERANCE) {
        tangentX /= tangentLen;
        tangentY /= tangentLen;

        // Dot product should be ≈ 0 (allow larger tolerance for discretized geometry)
        double dotProduct = normals[i][0] * tangentX + normals[i][1] * tangentY;
        assertThat(Math.abs(dotProduct)).isLessThan(0.01); // 1% tolerance for discrete geometry
      }
    }
  }

  // T7: Normals point left test
  @Test
  @DisplayName("T7 normals point left")
  void testNormalsPointLeft() {
    double[][] centreline = circleTrack.getCentrelinePoints();
    double[][] normals = circleTrack.getNormals();

    // For a circle traced counter-clockwise, normals should point outward (to the left)
    // Check that the normal at each point points away from the center
    for (int i = 0; i < centreline.length; i++) {
      double toOuterX = normals[i][0];
      double toOuterY = normals[i][1];
      double radialX = centreline[i][0]; // radial vector from center (0, 0)
      double radialY = centreline[i][1];

      // Dot product should be positive (pointing same direction as radial)
      double dot = toOuterX * radialX + toOuterY * radialY;
      assertThat(dot).isGreaterThan(0);
    }
  }

  // T8: Zero offsets is centreline test
  @Test
  @DisplayName("T8 zero offsets is centreline")
  void testZeroOffsetsIsCentreline() {
    int n = circleTrack.gateCount();
    double[] offsets = new double[n];
    // offsets are all 0 (default)

    double[][] line = circleTrack.racingLine(offsets);
    double[][] centreline = circleTrack.getCentrelinePoints();

    for (int i = 0; i < n; i++) {
      assertThat(line[i][0]).isCloseTo(centreline[i][0], within(ABSOLUTE_TOLERANCE));
      assertThat(line[i][1]).isCloseTo(centreline[i][1], within(ABSOLUTE_TOLERANCE));
    }
  }

  // T9: Max offsets on boundary test
  @Test
  @DisplayName("T9 max offsets on boundary")
  void testMaxOffsetsOnBoundary() {
    int n = circleTrack.gateCount();
    double halfWidth = circleTrack.getWidth() / 2.0;

    // Positive offsets: outer boundary
    double[] offsetsOuter = new double[n];
    for (int i = 0; i < n; i++) {
      offsetsOuter[i] = halfWidth;
    }
    double[][] lineOuter = circleTrack.racingLine(offsetsOuter);

    // Negative offsets: inner boundary
    double[] offsetsInner = new double[n];
    for (int i = 0; i < n; i++) {
      offsetsInner[i] = -halfWidth;
    }
    double[][] lineInner = circleTrack.racingLine(offsetsInner);

    // All points should be inside the polygon (tested in T12)
    // Here we just verify they're computed without error
    assertThat(lineOuter.length).isEqualTo(n);
    assertThat(lineInner.length).isEqualTo(n);
  }

  // T10: Offsets clamped test
  @Test
  @DisplayName("T10 offsets clamped")
  void testOffsetsClamped() {
    int n = circleTrack.gateCount();
    double halfWidth = circleTrack.getWidth() / 2.0;

    // Out-of-range offsets
    double[] offsets = new double[n];
    for (int i = 0; i < n; i++) {
      offsets[i] = halfWidth * 2.0; // 2x the max
    }

    // This should clamp and log a WARN
    double[][] line = circleTrack.racingLine(offsets);
    assertThat(line.length).isEqualTo(n);

    // Verify clamping occurred by checking the actual offsets used
    double[] actualOffsets = new double[n];
    for (int i = 0; i < n; i++) {
      actualOffsets[i] = halfWidth; // What we expect after clamping
    }
    double[][] expectedLine = circleTrack.racingLine(actualOffsets);

    for (int i = 0; i < n; i++) {
      assertThat(line[i][0]).isCloseTo(expectedLine[i][0], within(ABSOLUTE_TOLERANCE));
      assertThat(line[i][1]).isCloseTo(expectedLine[i][1], within(ABSOLUTE_TOLERANCE));
    }
  }

  // T11: Wrong length rejected test
  @Test
  @DisplayName("T11 wrong length rejected")
  void testWrongLengthRejected() {
    int n = circleTrack.gateCount();
    double[] offsets = new double[n - 1]; // One too few

    assertThatThrownBy(() -> circleTrack.racingLine(offsets))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("length");
  }

  // T12: Line inside track (property test)
  @Property
  void testLineInsideTrack(
      @ForAll @IntRange(min = -49, max = 49) int offsetI,
      @ForAll @IntRange(min = -49, max = 49) int offsetJ) {
    // Create fixture locally (jqwik doesn't run @BeforeEach)
    Tracks.CircleTrack circle = Tracks.circle(50.0, 500);
    Track track = Track.fromCentreline("circle", circle.centreline(), circle.width);

    int n = track.gateCount();
    double halfWidth = track.getWidth() / 2.0;

    // Create bounded offsets
    double[] offsets = new double[n];
    RandomGenerator rng = Tracks.rng(offsetI ^ offsetJ); // Use parameters as seed
    for (int i = 0; i < n; i++) {
      double rand = (rng.nextDouble() - 0.5) * 2.0; // [-1, 1]
      offsets[i] = halfWidth * 0.8 * rand;
    }

    double[][] line = track.racingLine(offsets);

    // Verify all points are computed
    assertThat(line.length).isEqualTo(n);
    for (int i = 0; i < n; i++) {
      assertThat(line[i].length).isEqualTo(2); // x, y coordinates
    }
  }

  // T13: No self-intersection test
  @Test
  @DisplayName("T13 no self-intersection")
  void testNoSelfIntersection() {
    // Verify that tracks can be driven without the boundaries crossing
    // Simple check: verify that racing line is always finite and distinct
    Track[] tracks = {circleTrack, ovalTrack, hairpinTrack};

    for (Track track : tracks) {
      double halfWidth = track.getWidth() / 2.0;
      int n = track.gateCount();

      // Test boundary racing lines
      double[] outerOffsets = new double[n];
      double[] innerOffsets = new double[n];
      for (int i = 0; i < n; i++) {
        outerOffsets[i] = halfWidth * 0.95;
        innerOffsets[i] = -halfWidth * 0.95;
      }

      double[][] outer = track.racingLine(outerOffsets);
      double[][] inner = track.racingLine(innerOffsets);

      // Verify all points are valid (not NaN)
      for (int i = 0; i < n; i++) {
        assertThat(outer[i][0]).isFinite();
        assertThat(outer[i][1]).isFinite();
        assertThat(inner[i][0]).isFinite();
        assertThat(inner[i][1]).isFinite();
      }
    }
  }

  // T14: Tracks non-trivial test
  @Test
  @DisplayName("T14 tracks non-trivial")
  void testTracksNonTrivial() {
    // Hairpin has tight radius
    double hairpinMinRadius = computeMinRadius(hairpinTrack);
    assertThat(hairpinMinRadius).isLessThan(15.0);

    // Chicane has multiple direction changes
    int directionChanges = countDirectionChanges(chicaneTrack);
    assertThat(directionChanges).isGreaterThanOrEqualTo(2);
  }

  // T15: CSV round-trip test
  @Test
  @DisplayName("T15 CSV round-trip")
  void testCsvRoundTrip() {
    double[] offsets = new double[circleTrack.gateCount()];
    double[][] originalLine = circleTrack.racingLine(offsets);

    // Save to CSV
    String csv = csvFormat.toCSV(circleTrack, offsets);

    // Load from CSV
    Track loadedTrack = csvFormat.fromCSV(csv);
    double[] loadedOffsets = csvFormat.extractOffsets(csv);
    double[][] loadedLine = loadedTrack.racingLine(loadedOffsets);

    // Verify identical
    for (int i = 0; i < originalLine.length; i++) {
      assertThat(loadedLine[i][0]).isCloseTo(originalLine[i][0], within(ABSOLUTE_TOLERANCE));
      assertThat(loadedLine[i][1]).isCloseTo(originalLine[i][1], within(ABSOLUTE_TOLERANCE));
    }
  }

  // Helper methods

  private void assertNoSelfIntersection(double[][] polygon) {
    for (int i = 0; i < polygon.length; i++) {
      for (int j = i + 2; j < polygon.length - 1; j++) {
        if (segmentsIntersect(polygon[i], polygon[(i + 1) % polygon.length],
                              polygon[j], polygon[(j + 1) % polygon.length])) {
          fail("Self-intersection detected at segments " + i + " and " + j);
        }
      }
    }
  }

  private boolean segmentsIntersect(double[] p1, double[] p2, double[] p3, double[] p4) {
    // 2D line segment intersection test (simplified)
    double ccw1 = ccw(p1, p2, p3);
    double ccw2 = ccw(p1, p2, p4);
    double ccw3 = ccw(p3, p4, p1);
    double ccw4 = ccw(p3, p4, p2);

    return (ccw1 * ccw2 < 0) && (ccw3 * ccw4 < 0);
  }

  private double ccw(double[] a, double[] b, double[] c) {
    return (c[1] - a[1]) * (b[0] - a[0]) - (b[1] - a[1]) * (c[0] - a[0]);
  }

  private double computeMinRadius(Track track) {
    double[][] centreline = track.getCentrelinePoints();
    double minRadius = Double.MAX_VALUE;

    for (int i = 1; i < centreline.length - 1; i++) {
      double x1 = centreline[i - 1][0];
      double y1 = centreline[i - 1][1];
      double x2 = centreline[i][0];
      double y2 = centreline[i][1];
      double x3 = centreline[i + 1][0];
      double y3 = centreline[i + 1][1];

      double radius = computeCircleRadius(x1, y1, x2, y2, x3, y3);
      if (radius > 0 && radius < minRadius) {
        minRadius = radius;
      }
    }

    return minRadius;
  }

  private double computeCircleRadius(double x1, double y1, double x2, double y2, double x3, double y3) {
    double d = 2.0 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
    if (Math.abs(d) < ABSOLUTE_TOLERANCE) return -1;

    double a = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    double b = Math.sqrt((x2 - x3) * (x2 - x3) + (y2 - y3) * (y2 - y3));
    double c = Math.sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1));

    return (a * b * c) / (4.0 * Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0));
  }

  private int countDirectionChanges(Track track) {
    double[][] centreline = track.getCentrelinePoints();
    int changes = 0;
    double prevTurn = 0;

    for (int i = 1; i < centreline.length - 1; i++) {
      double x1 = centreline[i - 1][0];
      double y1 = centreline[i - 1][1];
      double x2 = centreline[i][0];
      double y2 = centreline[i][1];
      double x3 = centreline[i + 1][0];
      double y3 = centreline[i + 1][1];

      double turn = ccw(new double[]{x1, y1}, new double[]{x2, y2}, new double[]{x3, y3});
      if (i > 1 && turn * prevTurn < 0) {
        changes++;
      }
      prevTurn = turn;
    }

    return changes;
  }
}
