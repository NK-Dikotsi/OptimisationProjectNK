package za.ac.uj.racinglines.track;

import java.util.HashMap;
import java.util.Map;

public class Centreline {
  private static final Map<String, CentrelineData> cache = new HashMap<>();

  public static class CentrelineData {
    public final int[] line;
    public final double lapTime;

    public CentrelineData(int[] line, double lapTime) {
      this.line = line;
      this.lapTime = lapTime;
    }
  }

  public static CentrelineData compute(RacingTrack track) {
    String cacheKey = track.getName();

    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }

    int[] centrelinePath = new int[track.getNumGates()];
    int centerNode = track.getNumNodes() / 2;

    for (int i = 0; i < track.getNumGates(); i++) {
      centrelinePath[i] = centerNode;
    }

    double lapTime = track.evaluateLine(centrelinePath);
    CentrelineData data = new CentrelineData(centrelinePath, lapTime);

    cache.put(cacheKey, data);

    return data;
  }

  public static int[] getCentrelinePath(RacingTrack track) {
    return compute(track).line;
  }

  public static double getCentrelineLapTime(RacingTrack track) {
    return compute(track).lapTime;
  }

  public static void clearCache() {
    cache.clear();
  }

  public static int getCacheSize() {
    return cache.size();
  }
}
