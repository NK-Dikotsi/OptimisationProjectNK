package za.ac.uj.racinglines.experiment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ExperimentConfig {
  private final String baseSeed;
  private final long budget;
  private final List<String> algorithms;
  private final List<String> tracks;
  private final List<Integer> seeds;

  public void validate() {
    if (budget <= 0) {
      throw new IllegalArgumentException("Budget must be positive");
    }
    if (algorithms == null || algorithms.isEmpty()) {
      throw new IllegalArgumentException("At least one algorithm required");
    }
    if (tracks == null || tracks.isEmpty()) {
      throw new IllegalArgumentException("At least one track required");
    }
    if (seeds == null || seeds.isEmpty()) {
      throw new IllegalArgumentException("At least one seed required");
    }
    for (String algo : algorithms) {
      if (!algo.matches("^(aco|pso|adaptive_evaporation)$")) {
        throw new IllegalArgumentException("Unknown algorithm: " + algo);
      }
    }
  }

  public int getTotalRuns() {
    return algorithms.size() * tracks.size() * seeds.size();
  }
}
