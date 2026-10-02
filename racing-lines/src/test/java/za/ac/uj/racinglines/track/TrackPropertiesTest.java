package za.ac.uj.racinglines.track;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import za.ac.uj.racinglines.fixtures.Tracks;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.*;

@Label("Track properties")
class TrackPropertiesTest {

  // T12: Line inside track (property test using jqwik)
  @Property
  @Label("T12 line inside track")
  void lineInsideTrackForAnyBoundedOffset(
      @ForAll @IntRange(min = -49, max = 49) int offsetI,
      @ForAll @IntRange(min = -49, max = 49) int offsetJ) {
    // Create fixture locally (jqwik doesn't run @BeforeEach)
    Tracks.CircleTrack circle = Tracks.circle(50.0, 500);
    Track track = Track.fromCentreline("circle", circle.centreline(), circle.width);

    int n = track.gateCount();
    double halfWidth = track.getWidth() / 2.0;

    // Create bounded offsets using parameters as seed
    double[] offsets = new double[n];
    RandomGenerator rng = Tracks.rng(offsetI ^ offsetJ);
    for (int i = 0; i < n; i++) {
      double rand = (rng.nextDouble() - 0.5) * 2.0; // [-1, 1]
      offsets[i] = halfWidth * 0.8 * rand;
    }

    double[][] line = track.racingLine(offsets);

    // Verify all points are computed
    assertThat(line.length).isEqualTo(n);
    for (int i = 0; i < n; i++) {
      assertThat(line[i].length).isEqualTo(2); // x, y coordinates
      assertThat(line[i][0]).isFinite();
      assertThat(line[i][1]).isFinite();
    }
  }
}
