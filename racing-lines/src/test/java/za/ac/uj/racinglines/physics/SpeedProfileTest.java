package za.ac.uj.racinglines.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import za.ac.uj.racinglines.track.Track;
import za.ac.uj.racinglines.track.TrackFactory;

/**
 * Speed-profile limits checked on the real experiment tracks. The old P9/P10 used a straight
 * line, where speed is constant at v_top, so they could not catch an ignored acceleration limit.
 */
class SpeedProfileTest {

  private static final PhysicsConfig CONFIG = new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0);
  private final LapTimeSimulator simulator = new LapTimeSimulator(CONFIG);

  static Stream<Arguments> tracks() {
    return Stream.of(
        Arguments.of("oval", (Supplier<Track>) TrackFactory::ovalStandard, 18.6255),
        Arguments.of("hairpin", (Supplier<Track>) TrackFactory::hairpinStandard, 17.0627),
        Arguments.of("chicane", (Supplier<Track>) TrackFactory::chicaneStandard, 21.3981));
  }

  private double[][] centreline(Track t) {
    return t.racingLine(new double[t.gateCount()]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tracks")
  @DisplayName("SPD1 speed never exceeds the friction limit, acceleration or braking limits")
  void limitsHold(String name, Supplier<Track> make, double expectedLap) {
    double[][] line = centreline(make.get());
    double[] s = simulator.segmentLengths(line);
    double[] vmax = simulator.maxSpeedByFriction(simulator.curvature(line));
    double[] v = simulator.speedProfile(vmax, s);
    int n = v.length;
    for (int i = 0; i < n; i++) {
      int next = (i + 1) % n;
      assertThat(v[i]).as("friction at %d", i).isLessThanOrEqualTo(vmax[i] + 1e-9);
      assertThat(v[next] * v[next] - v[i] * v[i]).as("acceleration at %d", i)
          .isLessThanOrEqualTo(2.0 * CONFIG.getAacc() * s[i] + 1e-6);
      assertThat(v[i] * v[i] - v[next] * v[next]).as("braking at %d", i)
          .isLessThanOrEqualTo(2.0 * CONFIG.getAbrake() * s[i] + 1e-6);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tracks")
  @DisplayName("SPD2 the car actually uses its acceleration (not stuck below the limits)")
  void profileIsTight(String name, Supplier<Track> make, double expectedLap) {
    double[][] line = centreline(make.get());
    double[] s = simulator.segmentLengths(line);
    double[] vmax = simulator.maxSpeedByFriction(simulator.curvature(line));
    double[] v = simulator.speedProfile(vmax, s);
    int n = v.length;
    // Every point is limited by something: friction, or reachable from the previous point,
    // or able to brake for the next point. Otherwise the profile is needlessly slow.
    for (int i = 0; i < n; i++) {
      int prev = Math.floorMod(i - 1, n);
      int next = (i + 1) % n;
      double accel = Math.sqrt(v[prev] * v[prev] + 2.0 * CONFIG.getAacc() * s[prev]);
      double brake = Math.sqrt(v[next] * v[next] + 2.0 * CONFIG.getAbrake() * s[i]);
      double bound = Math.min(vmax[i], Math.min(accel, brake));
      assertThat(v[i]).as("point %d", i).isCloseTo(bound, within(1e-6));
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tracks")
  @DisplayName("SPD3 centreline lap time regression (independent reference implementation)")
  void centrelineLap(String name, Supplier<Track> make, double expectedLap) {
    assertThat(simulator.lapTime(centreline(make.get())))
        .isCloseTo(expectedLap, within(expectedLap * 1e-3));
  }
}
