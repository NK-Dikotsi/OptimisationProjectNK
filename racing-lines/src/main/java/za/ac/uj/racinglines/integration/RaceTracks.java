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

  /** Physical track width in metres. */
  public static final double TRACK_WIDTH = 12.0;

  /** Car width in metres. The car's centre must stay half this from each edge. */
  public static final double CAR_WIDTH = 2.0;

  /** Width available to the car's centre: the offset bounds are +/- USABLE_WIDTH / 2. */
  public static final double USABLE_WIDTH = TRACK_WIDTH - CAR_WIDTH;

  private RaceTracks() {}

  public static RaceTrack byName(String name) {
    return switch (name) {
      case "oval" -> new RaceTrack(name, TrackFactory.ovalStandard(), USABLE_WIDTH);
      case "hairpin" -> new RaceTrack(name, TrackFactory.hairpinStandard(), USABLE_WIDTH);
      case "chicane" -> new RaceTrack(name, TrackFactory.chicaneStandard(), USABLE_WIDTH);
      default -> throw new IllegalArgumentException("Unknown track: " + name);
    };
  }
}
