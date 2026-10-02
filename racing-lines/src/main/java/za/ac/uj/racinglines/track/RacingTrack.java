package za.ac.uj.racinglines.track;

import java.util.List;

public interface RacingTrack {
  String getName();
  int getNumGates();
  int getNumNodes();
  double getHalfWidth();
  List<Gate> getGates();
  double getCentrelineLapTime();
  double evaluateLine(int[] line);
  double getSpeedProfile(int gateIndex, double offset);
}
