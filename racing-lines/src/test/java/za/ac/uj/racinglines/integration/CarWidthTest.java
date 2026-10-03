package za.ac.uj.racinglines.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;
import za.ac.uj.racinglines.track.Track;
import za.ac.uj.racinglines.track.TrackFactory;

/** Tests that the car's width constraint (2m car on 12m track) is enforced. */
class CarWidthTest {

  private static final PhysicsConfig CONFIG = new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0);
  private final LapTimeSimulator simulator = new LapTimeSimulator(CONFIG);
  private final OptimizationEngine engine =
      new OptimizationEngine(simulator, OptimizationEngine.Params.defaults());

  static Stream<Arguments> tracks() {
    return Stream.of(
        Arguments.of("oval", (Supplier<Track>) TrackFactory::ovalStandard),
        Arguments.of("hairpin", (Supplier<Track>) TrackFactory::hairpinStandard),
        Arguments.of("chicane", (Supplier<Track>) TrackFactory::chicaneStandard));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tracks")
  @DisplayName("CW1 usable width is wired through: track is 12m, car is 2m, so usable is 10m")
  void usableWidthWired(String trackName, Supplier<Track> makeTrack) {
    RaceTracks.RaceTrack rt = RaceTracks.byName(trackName);
    assertThat(RaceTracks.TRACK_WIDTH).isEqualTo(12.0);
    assertThat(RaceTracks.CAR_WIDTH).isEqualTo(2.0);
    assertThat(RaceTracks.USABLE_WIDTH).isEqualTo(10.0);
    assertThat(rt.width()).isEqualTo(10.0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tracks")
  @DisplayName("CW2 every best line keeps the car on the track (±5m from centreline)")
  void bestLineStaysOnTrack(String trackName, Supplier<Track> makeTrack) {
    RaceTracks.RaceTrack rt = RaceTracks.byName(trackName);
    RunOutcome result = engine.optimize("aco", trackName, rt.track(), rt.width(), 2000, 42);

    double[] bestOffsets = result.bestOffsets();
    double halfUsable = RaceTracks.USABLE_WIDTH / 2.0; // 5m

    for (int i = 0; i < bestOffsets.length; i++) {
      assertThat(bestOffsets[i])
          .as("gate %d", i)
          .isBetween(-halfUsable, halfUsable);
    }
  }
}
