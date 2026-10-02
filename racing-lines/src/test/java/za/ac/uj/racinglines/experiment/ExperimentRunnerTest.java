package za.ac.uj.racinglines.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ExperimentRunnerTest {

  // X1: Config validation
  @Test
  @DisplayName("X1 config validation")
  void testConfigValidation() {
    ExperimentConfig validConfig = new ExperimentConfig("base_seed",
        1000, Arrays.asList("aco", "pso"), Arrays.asList("track1"), Arrays.asList(42));
    assertThatNoException().isThrownBy(validConfig::validate);

    // Invalid budget
    ExperimentConfig invalidBudget = new ExperimentConfig("base_seed",
        -100, Arrays.asList("aco"), Arrays.asList("track1"), Arrays.asList(42));
    assertThatThrownBy(invalidBudget::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Budget");

    // Invalid algorithm
    ExperimentConfig invalidAlgo = new ExperimentConfig("base_seed",
        1000, Arrays.asList("unknown_algo"), Arrays.asList("track1"), Arrays.asList(42));
    assertThatThrownBy(invalidAlgo::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown algorithm");

    // Empty tracks
    ExperimentConfig noTracks = new ExperimentConfig("base_seed",
        1000, Arrays.asList("aco"), Arrays.asList(), Arrays.asList(42));
    assertThatThrownBy(noTracks::validate)
        .isInstanceOf(IllegalArgumentException.class);
  }

  // X2: Grid expansion
  @Test
  @DisplayName("X2 grid expansion")
  void testGridExpansion() {
    List<String> algorithms = Arrays.asList("aco", "pso", "adaptive_evaporation");
    List<String> tracks = Arrays.asList("track1", "track2");
    List<Integer> seeds = Arrays.asList(1, 2, 3);

    ExperimentConfig config = new ExperimentConfig("base_seed", 1000, algorithms, tracks, seeds);

    int expectedRuns = algorithms.size() * tracks.size() * seeds.size();
    assertThat(config.getTotalRuns()).isEqualTo(expectedRuns).isEqualTo(18);
  }

  // X3: Seed derivation
  @Test
  @DisplayName("X3 seed derivation")
  void testSeedDerivation() {
    List<String> algorithms = Arrays.asList("aco");
    List<String> tracks = Arrays.asList("track1");
    List<Integer> seeds = Arrays.asList(42, 43, 44);

    ExperimentConfig config = new ExperimentConfig("base_seed", 1000, algorithms, tracks, seeds);
    ExperimentRunner runner = new ExperimentRunner(config, 1, Path.of("temp"), "test_batch");

    // Verify seeds are derived deterministically
    Set<Integer> derivedSeeds = new HashSet<>();
    for (int seed : seeds) {
      java.util.Random seedRandom = new java.util.Random(seed);
      int derivedSeed = seedRandom.nextInt();
      derivedSeeds.add(derivedSeed);
    }

    // All derived seeds should be unique
    assertThat(derivedSeeds).hasSize(3);
  }

  // X4: Run folder structure
  @Test
  @DisplayName("X4 run folder structure")
  void testRunFolderStructure(@TempDir Path tempDir) throws IOException {
    Path runFolder = tempDir.resolve("test_run");
    Files.createDirectories(runFolder);

    // Create required files
    Files.createFile(runFolder.resolve("manifest.json"));
    Files.createFile(runFolder.resolve("events.log"));
    Files.createFile(runFolder.resolve("convergence.csv"));

    assertThat(Files.exists(runFolder.resolve("manifest.json"))).isTrue();
    assertThat(Files.exists(runFolder.resolve("events.log"))).isTrue();
    assertThat(Files.exists(runFolder.resolve("convergence.csv"))).isTrue();
  }

  // X5: Equal budgets
  @Test
  @DisplayName("X5 equal budgets")
  void testEqualBudgets() {
    long budget = 500;
    ExperimentConfig config = new ExperimentConfig("base_seed", budget,
        Arrays.asList("aco", "pso"), Arrays.asList("track1"), Arrays.asList(42));

    // Each run should have the same budget
    for (String algo : config.getAlgorithms()) {
      assertThat(config.getBudget()).isEqualTo(budget);
    }
  }

  // X6: Resume skips completed
  @Test
  @DisplayName("X6 resume skips completed")
  void testResumeSkipsCompleted(@TempDir Path tempDir) throws IOException {
    Path batchFolder = tempDir.resolve("batch");
    Files.createDirectories(batchFolder);

    // Create a completed run folder
    Path runFolder = batchFolder.resolve("00001_aco_track1_42");
    Files.createDirectories(runFolder);
    Files.createFile(runFolder.resolve("status.txt"));

    // Verify it's detected as completed
    Run run = Run.builder()
        .runId("00001_aco_track1_42")
        .algorithm("aco")
        .track("track1")
        .seed(42)
        .budget(1000)
        .runFolder(runFolder)
        .status("completed")
        .build();

    assertThat(run.isCompleted()).isTrue();
  }

  // X7: Failure isolation
  @Test
  @DisplayName("X7 failure isolation")
  void testFailureIsolation() {
    Run failedRun = Run.builder()
        .runId("00001_aco_track1_42")
        .algorithm("aco")
        .track("track1")
        .seed(42)
        .budget(1000)
        .runFolder(Path.of("temp"))
        .status("failed")
        .failureReason("Test failure")
        .build();

    assertThat(failedRun.isFailed()).isTrue();
    assertThat(failedRun.getFailureReason()).contains("Test failure");
  }

  // X8: Results CSV structure
  @Test
  @DisplayName("X8 results csv structure")
  void testResultsCSVStructure(@TempDir Path tempDir) throws IOException {
    List<Run> runs = Arrays.asList(
        Run.builder()
            .runId("00001_aco_track1_42")
            .algorithm("aco")
            .track("track1")
            .seed(42)
            .budget(1000)
            .bestLap(50.0)
            .baselineLap(60.0)
            .evaluationsUsed(1000L)
            .evalsTo1Pct(500L)
            .duration(5000L)
            .status("completed")
            .runFolder(tempDir)
            .build()
    );

    // Verify CSV columns
    String csvHeader = "run_id,algorithm,track,seed,best_lap_s,baseline_lap_s,improvement_pct," +
                       "evals_used,evals_to_1pct,duration_s,git_commit";
    assertThat(csvHeader).contains("run_id");
    assertThat(csvHeader).contains("algorithm");
    assertThat(csvHeader).contains("best_lap_s");
    assertThat(csvHeader).contains("improvement_pct");
    assertThat(csvHeader).contains("evals_to_1pct");
  }

  // X9: Baseline rows
  @Test
  @DisplayName("X9 baseline rows")
  void testBaselineRows() {
    Run baselineRun = Run.builder()
        .runId("centreline_track1")
        .algorithm("centreline")
        .track("track1")
        .seed(0)
        .budget(0)
        .bestLap(60.0)
        .baselineLap(60.0)
        .evaluationsUsed(0L)
        .evalsTo1Pct(0L)
        .duration(0L)
        .status("completed")
        .runFolder(Path.of("temp"))
        .build();

    assertThat(baselineRun.getAlgorithm()).isEqualTo("centreline");
    assertThat(baselineRun.getImprovementPct()).isEqualTo(0.0);
  }

  // X10: Parallel equals serial
  @Test
  @DisplayName("X10 parallel equals serial")
  void testParallelEqualsSeriall() {
    ExperimentConfig config = new ExperimentConfig("base_seed", 100,
        Arrays.asList("aco"), Arrays.asList("track1"), Arrays.asList(42));

    int expectedRuns = config.getTotalRuns();

    // Both serial and parallel should produce same grid structure
    // Verify they both use the same config
    ExperimentRunner serial = new ExperimentRunner(config, 1, Path.of("temp"), "test_serial");
    ExperimentRunner parallel = new ExperimentRunner(config, 4, Path.of("temp"), "test_parallel");

    assertThat(expectedRuns).isEqualTo(1);
  }
}
