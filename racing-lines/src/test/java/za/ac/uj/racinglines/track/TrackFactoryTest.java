package za.ac.uj.racinglines.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests the tracks the experiments actually run on (not the test fixtures). */
class TrackFactoryTest {

  static Stream<Arguments> standardTracks() {
    return Stream.of(
        Arguments.of("oval", (Supplier<Track>) TrackFactory::ovalStandard,
            2 * 150 + 2 * Math.PI * 30),
        Arguments.of("hairpin", (Supplier<Track>) TrackFactory::hairpinStandard,
            2 * 150 + 2 * Math.PI * 12),
        Arguments.of("chicane", (Supplier<Track>) TrackFactory::chicaneStandard,
            // bottom: 2 lead straights + 3 arcs (45+90+45 deg at R 20); top 150; two R30 ends
            (150 - 4 * 20 * Math.sin(Math.toRadians(45))) + 20 * Math.PI + 150 + 2 * Math.PI * 30));
  }

  private static double[][] centreline(Track t) {
    return t.racingLine(new double[t.gateCount()]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("standardTracks")
  @DisplayName("TF1 track is a closed loop with even gate spacing (no closing jump)")
  void closedAndEven(String name, Supplier<Track> make, double expectedLength) {
    double[] spacing = make.get().gateSpacings();
    double min = Double.MAX_VALUE;
    double max = 0;
    for (double s : spacing) {
      min = Math.min(min, s);
      max = Math.max(max, s);
    }
    // An open path closed artificially gives one spacing many times the others.
    assertThat(max / min).isLessThan(1.02);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("standardTracks")
  @DisplayName("TF2 centreline length matches the designed geometry")
  void lengthMatchesDesign(String name, Supplier<Track> make, double expectedLength) {
    double length = 0;
    for (double s : make.get().gateSpacings()) {
      length += s;
    }
    assertThat(length).isCloseTo(expectedLength, within(expectedLength * 0.01));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("standardTracks")
  @DisplayName("TF3 centreline never crosses itself")
  void noSelfIntersection(String name, Supplier<Track> make, double expectedLength) {
    double[][] p = centreline(make.get());
    int n = p.length;
    for (int i = 0; i < n; i++) {
      for (int j = i + 2; j < n; j++) {
        if (i == 0 && j == n - 1) {
          continue; // adjacent through the wrap-around
        }
        assertThat(segmentsCross(p[i], p[(i + 1) % n], p[j], p[(j + 1) % n]))
            .as("segments %d and %d cross", i, j).isFalse();
      }
    }
  }

  private static boolean segmentsCross(double[] a, double[] b, double[] c, double[] d) {
    double d1 = cross(c, d, a);
    double d2 = cross(c, d, b);
    double d3 = cross(a, b, c);
    double d4 = cross(a, b, d);
    return d1 * d2 < 0 && d3 * d4 < 0;
  }

  private static double cross(double[] o, double[] a, double[] b) {
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
  }
}
