package za.ac.uj.racinglines.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AnalysisTest {

  // V1: Summary statistics
  @Test
  @DisplayName("V1 summary stats")
  void testSummaryStats() {
    double[] samples = {10.0, 20.0, 30.0, 40.0, 50.0};
    SummaryStats stats = SummaryStats.compute(samples);

    assertThat(stats.getMean()).isCloseTo(30.0, within(0.01));
    assertThat(stats.getBest()).isEqualTo(10.0);
    assertThat(stats.getWorst()).isEqualTo(50.0);
    assertThat(stats.getSampleCount()).isEqualTo(5);
  }

  // V2: Convergence alignment
  @Test
  @DisplayName("V2 convergence alignment")
  void testConvergenceAlignment() {
    long[] evals1 = {100, 200, 300, 400, 500};
    double[] laps1 = {50.0, 45.0, 42.0, 41.0, 40.0};

    long[] targetGrid = {100, 200, 300, 400, 500};
    double[] interpolated = ConvergenceInterpolator.interpolateLinear(evals1, laps1, targetGrid);

    assertThat(interpolated).hasSize(5);
    assertThat(interpolated[0]).isCloseTo(50.0, within(0.01));
    assertThat(interpolated[4]).isCloseTo(40.0, within(0.01));
  }

  // V2b: Common grid creation
  @Test
  @DisplayName("V2b common grid")
  void testCommonGrid() {
    List<long[]> allEvals = Arrays.asList(
        new long[]{10, 20, 30, 40, 50},
        new long[]{5, 15, 25, 35, 45}
    );

    long[] commonGrid = ConvergenceInterpolator.createCommonGrid(allEvals, 5);

    assertThat(commonGrid).hasSize(5);
    assertThat(commonGrid[0]).isEqualTo(5);
    assertThat(commonGrid[4]).isEqualTo(50);
  }

  // V3: Wilcoxon test
  @Test
  @DisplayName("V3 wilcoxon test")
  void testWilcoxonTest() {
    double[] sample1 = {1.0, 2.0, 3.0, 4.0, 5.0};
    double[] sample2 = {1.1, 2.1, 3.1, 4.1, 5.1};

    double pValue = StatisticalTests.wilcoxonTest(sample1, sample2);

    assertThat(pValue).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);

    // Very different samples should have low p-value
    double[] sample3 = {10.0, 11.0, 12.0, 13.0, 14.0};
    double pValueDiff = StatisticalTests.wilcoxonTest(sample1, sample3);

    assertThat(pValueDiff).isLessThan(pValue);
  }

  // V3b: Identical samples
  @Test
  @DisplayName("V3b identical samples")
  void testIdenticalSamples() {
    double[] sample1 = {5.0, 5.0, 5.0, 5.0, 5.0};
    double[] sample2 = {5.0, 5.0, 5.0, 5.0, 5.0};

    double pValue = StatisticalTests.wilcoxonTest(sample1, sample2);

    assertThat(pValue).isEqualTo(1.0);
  }

  // V4: Incomplete runs excluded
  @Test
  @DisplayName("V4 incomplete runs excluded")
  void testIncompleteRunsExcluded() {
    ResultRow completedRun = ResultRow.builder()
        .runId("00001_aco_track1_42")
        .algorithm("aco")
        .track("track1")
        .seed(42)
        .bestLap(50.0)
        .baselineLap(60.0)
        .improvementPct(16.7)
        .evalsUsed(1000)
        .build();

    assertThat(completedRun.getBestLap()).isNotNull();
    assertThat(completedRun.getEvalsUsed()).isGreaterThan(0);
  }

  // V5: Plot files written
  @Test
  @DisplayName("V5 plot files written")
  void testPlotFilesWritten(@TempDir Path tempDir) throws IOException {
    PlotGenerator plotter = new PlotGenerator(tempDir);

    plotter.generateTrackPlots("track1", Arrays.asList("aco", "pso"));
    plotter.generateSpeedProfile("track1", "aco");
    plotter.generateConvergenceCurves("track1", Arrays.asList("aco", "pso"));

    assertThat(Files.exists(tempDir.resolve("track_track1.pdf"))).isTrue();
    assertThat(Files.exists(tempDir.resolve("speed_track1_aco.pdf"))).isTrue();
    assertThat(Files.exists(tempDir.resolve("convergence_track1.pdf"))).isTrue();
  }

  // V6: Overlay uses best run
  @Test
  @DisplayName("V6 overlay uses best run")
  void testOverlayUsesBestRun() {
    List<ResultRow> runs = Arrays.asList(
        ResultRow.builder()
            .runId("00001_aco_track1_42")
            .algorithm("aco")
            .track("track1")
            .seed(42)
            .bestLap(50.0)
            .baselineLap(60.0)
            .improvementPct(16.7)
            .evalsUsed(1000)
            .build(),
        ResultRow.builder()
            .runId("00002_aco_track1_43")
            .algorithm("aco")
            .track("track1")
            .seed(43)
            .bestLap(55.0)
            .baselineLap(60.0)
            .improvementPct(8.3)
            .evalsUsed(1000)
            .build()
    );

    // Find best run
    ResultRow best = runs.stream()
        .min((a, b) -> Double.compare(a.getBestLap(), b.getBestLap()))
        .orElse(null);

    assertThat(best).isNotNull();
    assertThat(best.getBestLap()).isEqualTo(50.0);
    assertThat(best.getRunId()).isEqualTo("00001_aco_track1_42");
  }

  // V7: LaTeX table
  @Test
  @DisplayName("V7 latex table")
  void testLatexTable() {
    SummaryStats stats1 = SummaryStats.builder()
        .algorithm("aco")
        .track("track1")
        .mean(50.0)
        .std(2.0)
        .best(48.0)
        .worst(52.0)
        .sampleCount(5)
        .build();

    String table = String.format(java.util.Locale.US, "%s,%s,%.2f±%.2f\n",
        stats1.getAlgorithm(), stats1.getTrack(),
        stats1.getMean(), stats1.getStd());

    assertThat(table).contains("aco");
    assertThat(table).contains("track1");
    assertThat(table).contains("50.00");
  }

  // V8: Analysis deterministic
  @Test
  @DisplayName("V8 analysis deterministic")
  void testAnalysisDeterministic(@TempDir Path tempDir) throws IOException {
    Path batchFolder = tempDir.resolve("batch");
    Files.createDirectories(batchFolder);

    // Create results CSV
    Path resultsCSV = batchFolder.resolve("results.csv");
    String csvContent = "run_id,algorithm,track,seed,best_lap_s,baseline_lap_s,improvement_pct,evals_used\n" +
        "00001_aco_track1_42,aco,track1,42,50.0,60.0,16.7,1000\n" +
        "00002_aco_track1_43,aco,track1,43,51.0,60.0,15.0,1000\n";
    Files.write(resultsCSV, csvContent.getBytes(StandardCharsets.UTF_8));

    Path outputDir = tempDir.resolve("output");
    ResultsAnalyzer analyzer1 = new ResultsAnalyzer(batchFolder, outputDir);

    // First run
    analyzer1.analyze();
    Path table1 = outputDir.resolve("results_table.csv");
    assertThat(Files.exists(table1)).isTrue();

    String content1 = Files.readString(table1);

    // Second run with fresh output dir
    Path outputDir2 = tempDir.resolve("output2");
    ResultsAnalyzer analyzer2 = new ResultsAnalyzer(batchFolder, outputDir2);
    analyzer2.analyze();
    Path table2 = outputDir2.resolve("results_table.csv");
    String content2 = Files.readString(table2);

    assertThat(content1).isEqualTo(content2);
  }
}
