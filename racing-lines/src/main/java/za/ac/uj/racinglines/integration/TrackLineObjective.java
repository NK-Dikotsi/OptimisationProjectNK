package za.ac.uj.racinglines.integration;

import za.ac.uj.racinglines.aco.LineObjective;
import za.ac.uj.racinglines.track.RacingTrack;

public class TrackLineObjective implements LineObjective {
  private final RacingTrack track;

  public TrackLineObjective(RacingTrack track) {
    this.track = track;
  }

  @Override
  public double evaluateLine(int[] line) {
    return track.evaluateLine(line);
  }

  @Override
  public int getNumGates() {
    return track.getNumGates();
  }

  @Override
  public int getNumNodes() {
    return track.getNumNodes();
  }

  @Override
  public double getHalfWidth() {
    return track.getHalfWidth();
  }

  public RacingTrack getTrack() {
    return track;
  }
}
