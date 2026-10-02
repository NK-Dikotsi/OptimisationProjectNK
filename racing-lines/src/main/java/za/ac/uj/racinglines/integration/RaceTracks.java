package za.ac.uj.racinglines.integration;

import za.ac.uj.racinglines.track.Track;
import za.ac.uj.racinglines.track.TrackFactory;

/**
 * Maps the track names used in configs/main.yaml to real {@link Track} geometry with their
 * track widths (needed for offset bounds).
 */
public final class RaceTracks {

  /** A track plus its width in metres (needed for the offset bounds). */
  public record RaceTrack(String name, Track track, double width) {}

  private RaceTracks() {}

  public static RaceTrack byName(String name) {
    return switch (name) {
      case "oval" -> new RaceTrack(name, TrackFactory.ovalStandard(), 12.0);
      case "hairpin" -> new RaceTrack(name, TrackFactory.hairpinStandard(), 12.0);
      case "chicane" -> new RaceTrack(name, TrackFactory.chicaneStandard(), 12.0);
      default -> throw new IllegalArgumentException("Unknown track: " + name);
    };
  }
}
