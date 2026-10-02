package za.ac.uj.racinglines.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SummaryStats {
  private String algorithm;
  private String track;
  private final double mean;
  private final double std;
  private final double best;
  private final double worst;
  private final int sampleCount;

  public static SummaryStats compute(double[] samples) {
    if (samples.length == 0) {
      return SummaryStats.builder()
          .mean(0)
          .std(0)
          .best(0)
          .worst(0)
          .sampleCount(0)
          .build();
    }

    double sum = 0;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    for (double sample : samples) {
      sum += sample;
      min = Math.min(min, sample);
      max = Math.max(max, sample);
    }

    double mean = sum / samples.length;
    double varianceSum = 0;
    for (double sample : samples) {
      varianceSum += Math.pow(sample - mean, 2);
    }
    double std = Math.sqrt(varianceSum / samples.length);

    return SummaryStats.builder()
        .mean(mean)
        .std(std)
        .best(min)
        .worst(max)
        .sampleCount(samples.length)
        .build();
  }
}
