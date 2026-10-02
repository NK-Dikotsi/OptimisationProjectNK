package za.ac.uj.racinglines.analysis;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ConvergenceInterpolator {

  public static double[] interpolateLinear(long[] evals, double[] laps, long[] targetEvals) {
    double[] result = new double[targetEvals.length];

    for (int i = 0; i < targetEvals.length; i++) {
      long target = targetEvals[i];

      if (target <= evals[0]) {
        result[i] = laps[0];
      } else if (target >= evals[evals.length - 1]) {
        result[i] = laps[laps.length - 1];
      } else {
        for (int j = 0; j < evals.length - 1; j++) {
          if (evals[j] <= target && target <= evals[j + 1]) {
            double fraction = (double) (target - evals[j]) / (evals[j + 1] - evals[j]);
            result[i] = laps[j] + fraction * (laps[j + 1] - laps[j]);
            break;
          }
        }
      }
    }

    return result;
  }

  public static long[] createCommonGrid(List<long[]> allEvals, int gridSize) {
    long minEval = Long.MAX_VALUE;
    long maxEval = Long.MIN_VALUE;

    for (long[] evals : allEvals) {
      if (evals.length > 0) {
        minEval = Math.min(minEval, evals[0]);
        maxEval = Math.max(maxEval, evals[evals.length - 1]);
      }
    }

    long[] grid = new long[gridSize];
    for (int i = 0; i < gridSize; i++) {
      grid[i] = minEval + (long) ((maxEval - minEval) * i / (gridSize - 1));
    }

    return grid;
  }
}
