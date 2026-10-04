package za.ac.uj.racinglines.realtrack;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.track.Track;

class TumTrackTest {

  private static final String TEST_CSV = "data/Spielberg.csv";

  // RT1: Load a real track from CSV
  @Test
  void rt1_loadTrackFromCsv() throws IOException {
    Path csv = Paths.get(TEST_CSV);
    assertTrue(Files.exists(csv), "Test data file " + csv + " not found");

    TumTrack track = TumTrack.load("Spielberg", csv);
    assertNotNull(track);
    assertEquals("Spielberg", track.name());
    assertTrue(track.points() > 10, "Track should have at least 10 points");
  }

  // RT2: Track length computation
  @Test
  void rt2_trackLengthComputation() throws IOException {
    TumTrack track = TumTrack.load("Spielberg", Paths.get(TEST_CSV));
    double length = track.lengthMetres();

    assertTrue(length > 1000, "Spielberg track should be longer than 1 km");
    assertTrue(length < 10000, "Spielberg track should be shorter than 10 km");
  }

  // RT3: Track width extraction
  @Test
  void rt3_trackWidthExtraction() throws IOException {
    TumTrack track = TumTrack.load("Spielberg", Paths.get(TEST_CSV));
    double width = track.trackWidth();

    assertTrue(width > 0, "Track width should be positive");
    assertTrue(width < 50, "Realistic track width should be less than 50 m");
    assertEquals(2.0 * track.minHalfWidth(), width, 1e-9);
  }

  // RT4: Min and median half-widths
  @Test
  void rt4_minAndMedianHalfWidths() throws IOException {
    TumTrack track = TumTrack.load("Spielberg", Paths.get(TEST_CSV));
    double minHalf = track.minHalfWidth();
    double medianHalf = track.medianHalfWidth();

    assertTrue(minHalf > 0, "Min half-width should be positive");
    assertTrue(medianHalf > 0, "Median half-width should be positive");
    assertTrue(medianHalf >= minHalf, "Median should be >= minimum");
  }

  // RT5: Conversion to internal Track model
  @Test
  void rt5_conversionToTrackModel() throws IOException {
    TumTrack tum = TumTrack.load("Spielberg", Paths.get(TEST_CSV));
    Track track = tum.toTrack();

    assertNotNull(track);
    assertEquals("Spielberg", track.getName());
    assertTrue(track.gateCount() > 0, "Track should have gates");
    assertEquals(tum.points(), track.gateCount(), "Gate count should match centreline points");
  }

  // RT6: Polyline loading (reference race line)
  @Test
  void rt6_polylineLoading() throws IOException {
    Path raceline = Paths.get("data/Spielberg_raceline.csv");
    assertTrue(Files.exists(raceline), "Test reference raceline file not found");

    double[][] polyline = TumTrack.loadPolyline(raceline);
    assertNotNull(polyline);
    assertTrue(polyline.length > 10, "Reference raceline should have at least 10 points");

    for (double[] point : polyline) {
      assertEquals(2, point.length, "Each point should have x, y coordinates");
    }
  }
}
