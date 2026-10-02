package za.ac.uj.racinglines.track;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CentrelineTest {

  @BeforeEach
  void setUp() {
    Centreline.clearCache();
  }

  // C1: Compute centreline for oval
  @Test
  @DisplayName("C1 compute oval centreline")
  void testComputeOvalCentreline() {
    RacingTrack track = new OvalTrack();
    Centreline.CentrelineData data = Centreline.compute(track);

    assertThat(data).isNotNull();
    assertThat(data.line).hasSize(track.getNumGates());
    assertThat(data.lapTime).isFinite().isGreaterThan(0);
  }

  // C2: Centreline path uses center nodes
  @Test
  @DisplayName("C2 centreline uses center nodes")
  void testCentrelineUsesCenter() {
    RacingTrack track = new OvalTrack();
    int[] path = Centreline.getCentrelinePath(track);

    int expectedCenter = track.getNumNodes() / 2;
    for (int node : path) {
      assertThat(node).isEqualTo(expectedCenter);
    }
  }

  // C3: Caching works
  @Test
  @DisplayName("C3 caching")
  void testCaching() {
    RacingTrack track1 = new OvalTrack();
    RacingTrack track2 = new OvalTrack();

    Centreline.CentrelineData data1 = Centreline.compute(track1);
    int cacheSize1 = Centreline.getCacheSize();

    Centreline.CentrelineData data2 = Centreline.compute(track2);
    int cacheSize2 = Centreline.getCacheSize();

    // Same track name should use cache
    assertThat(cacheSize1).isEqualTo(1);
    assertThat(cacheSize2).isEqualTo(1);
    assertThat(data1.lapTime).isEqualTo(data2.lapTime);
  }

  // C4: Different tracks different centrelines
  @Test
  @DisplayName("C4 different tracks")
  void testDifferentTracks() {
    RacingTrack oval = new OvalTrack();
    RacingTrack tight = new TightTrack();
    RacingTrack road = new RoadTrack();

    double ovalCentreline = Centreline.getCentrelineLapTime(oval);
    double tightCentreline = Centreline.getCentrelineLapTime(tight);
    double roadCentreline = Centreline.getCentrelineLapTime(road);

    assertThat(ovalCentreline).isNotEqualTo(tightCentreline);
    assertThat(ovalCentreline).isNotEqualTo(roadCentreline);
    assertThat(tightCentreline).isNotEqualTo(roadCentreline);
  }

  // C5: Centreline is best possible for center-focused metric
  @Test
  @DisplayName("C5 centreline is baseline")
  void testCentrelineIsBaseline() {
    RacingTrack track = new OvalTrack();

    int[] centrelinePath = Centreline.getCentrelinePath(track);
    double centrelineLap = track.evaluateLine(centrelinePath);

    // Create a random path with lateral movement
    int[] randomPath = new int[track.getNumGates()];
    for (int i = 0; i < track.getNumGates(); i++) {
      randomPath[i] = (i % 2 == 0) ? 0 : track.getNumNodes() - 1;
    }
    double randomLap = track.evaluateLine(randomPath);

    // Centreline should be better than erratic path
    assertThat(centrelineLap).isLessThan(randomLap);
  }

  // C6: Cache can be cleared
  @Test
  @DisplayName("C6 cache clear")
  void testCacheClear() {
    RacingTrack track = new OvalTrack();

    Centreline.compute(track);
    assertThat(Centreline.getCacheSize()).isEqualTo(1);

    Centreline.clearCache();
    assertThat(Centreline.getCacheSize()).isEqualTo(0);

    Centreline.compute(track);
    assertThat(Centreline.getCacheSize()).isEqualTo(1);
  }

  // C7: Tight track stricter than oval
  @Test
  @DisplayName("C7 tight stricter than oval")
  void testTightStricterThanOval() {
    OvalTrack oval = new OvalTrack();
    TightTrack tight = new TightTrack();

    // Both have same center path
    int[] ovalPath = Centreline.getCentrelinePath(oval);
    int[] tightPath = Centreline.getCentrelinePath(tight);

    // But tight track penalizes any deviation more
    // Create slight lateral movement
    int[] ovalDeviated = ovalPath.clone();
    ovalDeviated[0] = (ovalPath[0] + 1) % oval.getNumNodes();

    int[] tightDeviated = tightPath.clone();
    tightDeviated[0] = (tightPath[0] + 1) % tight.getNumNodes();

    double ovalPenalty = oval.evaluateLine(ovalDeviated) - oval.evaluateLine(ovalPath);
    double tightPenalty = tight.evaluateLine(tightDeviated) - tight.evaluateLine(tightPath);

    // Tight track should penalize deviation more (in relative terms)
    assertThat(tightPenalty / tight.getCentrelineLapTime())
        .isGreaterThan(ovalPenalty / oval.getCentrelineLapTime());
  }

  // C8: Centreline lap consistent
  @Test
  @DisplayName("C8 centreline consistency")
  void testCentrelineConsistency() {
    RacingTrack track = new OvalTrack();

    double lap1 = Centreline.getCentrelineLapTime(track);
    double lap2 = Centreline.getCentrelineLapTime(track);
    double lap3 = Centreline.getCentrelineLapTime(track);

    assertThat(lap1).isEqualTo(lap2).isEqualTo(lap3);
  }

  // C9: All tracks have valid centrelines
  @Test
  @DisplayName("C9 all tracks valid centrelines")
  void testAllTracksValidCentrelines() {
    RacingTrack[] tracks = {new OvalTrack(), new TightTrack(), new RoadTrack()};

    for (RacingTrack track : tracks) {
      double centrelineLap = Centreline.getCentrelineLapTime(track);
      assertThat(centrelineLap).isFinite().isGreaterThan(0);
    }
  }

  // C10: Centreline better than random
  @Test
  @DisplayName("C10 centreline vs random")
  void testCentrelineVsRandom() {
    RacingTrack track = new OvalTrack();

    int[] centrelinePath = Centreline.getCentrelinePath(track);
    double centrelineLap = track.evaluateLine(centrelinePath);

    // Random path with extreme lateral movement
    int[] extremePath = new int[track.getNumGates()];
    for (int i = 0; i < track.getNumGates(); i++) {
      extremePath[i] = (i / 2) % track.getNumNodes();
    }
    double extremeLap = track.evaluateLine(extremePath);

    assertThat(centrelineLap).isLessThan(extremeLap);
  }
}
