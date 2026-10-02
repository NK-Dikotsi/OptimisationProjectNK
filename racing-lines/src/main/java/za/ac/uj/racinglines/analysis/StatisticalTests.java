package za.ac.uj.racinglines.analysis;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StatisticalTests {

  public static double wilcoxonTest(double[] sample1, double[] sample2) {
    if (sample1.length == 0 || sample2.length == 0) {
      return 1.0;
    }

    // Simple two-sample t-test approximation for Wilcoxon
    double mean1 = computeMean(sample1);
    double mean2 = computeMean(sample2);

    double var1 = computeVariance(sample1, mean1);
    double var2 = computeVariance(sample2, mean2);

    double pooledStd = Math.sqrt((var1 + var2) / 2);
    if (pooledStd == 0) {
      return mean1 == mean2 ? 1.0 : 0.0;
    }

    double tStatistic = Math.abs(mean1 - mean2) / (pooledStd * Math.sqrt(1.0 / sample1.length + 1.0 / sample2.length));

    // Approximate p-value using t-distribution approximation
    // For large samples, use normal approximation
    // For small samples, use a rough approximation
    int df = sample1.length + sample2.length - 2;
    return approximatePValue(tStatistic, df);
  }

  private static double computeMean(double[] values) {
    double sum = 0;
    for (double v : values) {
      sum += v;
    }
    return sum / values.length;
  }

  private static double computeVariance(double[] values, double mean) {
    double sum = 0;
    for (double v : values) {
      sum += Math.pow(v - mean, 2);
    }
    return sum / values.length;
  }

  private static double approximatePValue(double tStatistic, int df) {
    // Very rough approximation of p-value from t-statistic
    // This is a simplified version; real implementation would use t-distribution
    if (tStatistic < 1.0) {
      return Math.max(0.05, 1.0 - (tStatistic / 4));
    } else if (tStatistic < 2.0) {
      return 0.05;
    } else {
      return 0.01;
    }
  }

  public static boolean isSignificant(double pValue, double alpha) {
    return pValue < alpha;
  }
}
