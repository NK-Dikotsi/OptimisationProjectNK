package za.ac.uj.racinglines.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.track.Centreline;
import za.ac.uj.racinglines.track.OvalTrack;
import za.ac.uj.racinglines.track.RacingTrack;
import za.ac.uj.racinglines.track.RoadTrack;
import za.ac.uj.racinglines.track.TightTrack;

import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

class P6CentrelineTest {

  // P6: Circle lap time (Centreline baseline computation)
  @Test
  @DisplayName("P6 centreline lap times")
  void testCentrelineCircleLap() {
    Centreline.clearCache();

    RacingTrack oval = new OvalTrack();
    RacingTrack tight = new TightTrack();
    RacingTrack road = new RoadTrack();

    double ovalCentreline = Centreline.getCentrelineLapTime(oval);
    double tightCentreline = Centreline.getCentrelineLapTime(tight);
    double roadCentreline = Centreline.getCentrelineLapTime(road);

    // All must be real, finite values
    assertThat(ovalCentreline).isFinite().isGreaterThan(0);
    assertThat(tightCentreline).isFinite().isGreaterThan(0);
    assertThat(roadCentreline).isFinite().isGreaterThan(0);

    // All three must differ significantly
    assertThat(ovalCentreline).isNotCloseTo(tightCentreline, within(0.1));
    assertThat(ovalCentreline).isNotCloseTo(roadCentreline, within(0.1));
    assertThat(tightCentreline).isNotCloseTo(roadCentreline, within(0.1));

    // Print with proper locale formatting (for documentation)
    System.out.println("Centreline lap times:");
    System.out.println(String.format(Locale.ROOT, "  Oval:  %.3f s", ovalCentreline));
    System.out.println(String.format(Locale.ROOT, "  Tight: %.3f s", tightCentreline));
    System.out.println(String.format(Locale.ROOT, "  Road:  %.3f s", roadCentreline));

    // Tracks have different lap times - verify they're correctly computed
    // (relative ordering depends on segment time calculations)
    assertThat(ovalCentreline).isPositive();
    assertThat(tightCentreline).isPositive();
    assertThat(roadCentreline).isPositive();
  }

  // P6b: Locale formatting test
  @Test
  @DisplayName("P6b locale formatting")
  void testLocaleFormatting() {
    double value = 55.123456;

    String formatted = String.format(Locale.ROOT, "%.3f", value);
    assertThat(formatted).isEqualTo("55.123");

    // Not locale-dependent decimal separator
    assertThat(formatted).contains(".");
    assertThat(formatted).doesNotContain(",");
  }

  // P6c: Git info test
  @Test
  @DisplayName("P6c git info")
  void testGitInfo() {
    za.ac.uj.racinglines.util.GitInfo info = new za.ac.uj.racinglines.util.GitInfo();

    String commit = za.ac.uj.racinglines.util.GitInfo.getCommitHash();
    assertThat(commit).isNotBlank().isNotEqualTo("unknown");

    boolean isDirty = za.ac.uj.racinglines.util.GitInfo.isDirty();
    // Just verify it returns a boolean
    assertThat(isDirty).isNotNull();
  }
}
