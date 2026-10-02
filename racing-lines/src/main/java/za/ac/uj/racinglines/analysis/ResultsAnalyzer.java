package za.ac.uj.racinglines.analysis;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
public class ResultsAnalyzer {
  private static final Logger logger = LoggerFactory.getLogger(ResultsAnalyzer.class);

  private final Path batchFolder;
  private final Path outputDir;

  public void analyze() throws IOException {
    long startTime = System.currentTimeMillis();
    Path resultsCSV = batchFolder.resolve("results.csv");

    if (!Files.exists(resultsCSV)) {
      throw new IOException("results.csv not found in batch folder");
    }

    List<ResultRow> results = parseResults(resultsCSV);
    logger.info("Analysis start", kv("batch_path", batchFolder.toString()),
        kv("runs_found", results.size()));

    // Compute summary statistics
    Map<String, SummaryStats> stats = computeSummaryStats(results);

    // Generate plots
    PlotGenerator plotter = new PlotGenerator(outputDir);
    Set<String> tracks = extractTracks(results);
    Set<String> algorithms = extractAlgorithms(results);

    for (String track : tracks) {
      plotter.generateTrackPlots(track, new ArrayList<>(algorithms));
      plotter.generateConvergenceCurves(track, new ArrayList<>(algorithms));
      plotter.generateDiversityCurves(track, new ArrayList<>(algorithms));
      plotter.generateBoxPlots(track, new ArrayList<>(algorithms));
    }

    plotter.generateSensitivityPlots("swarm_size");
    plotter.generateSensitivityPlots("w");
    plotter.generateSensitivityPlots("rho");

    // Export results table
    exportResultsTable(stats);

    long duration = System.currentTimeMillis() - startTime;
    logger.info("Analysis end", kv("duration_s", duration / 1000.0));
  }

  private List<ResultRow> parseResults(Path resultsCSV) throws IOException {
    List<ResultRow> results = new ArrayList<>();
    List<String> lines = Files.readAllLines(resultsCSV);

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      String[] parts = line.split(",");
      if (parts.length >= 8) {
        ResultRow row = ResultRow.builder()
            .runId(parts[0])
            .algorithm(parts[1])
            .track(parts[2])
            .seed(Integer.parseInt(parts[3]))
            .bestLap(Double.parseDouble(parts[4]))
            .baselineLap(Double.parseDouble(parts[5]))
            .improvementPct(Double.parseDouble(parts[6]))
            .evalsUsed(Long.parseLong(parts[7]))
            .build();
        results.add(row);
      }
    }

    return results;
  }

  private Map<String, SummaryStats> computeSummaryStats(List<ResultRow> results) {
    Map<String, SummaryStats> stats = new HashMap<>();
    Map<String, List<Double>> grouped = new HashMap<>();

    for (ResultRow row : results) {
      String key = row.getAlgorithm() + "_" + row.getTrack();
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row.getBestLap());
    }

    for (String key : grouped.keySet()) {
      List<Double> values = grouped.get(key);
      double[] array = values.stream().mapToDouble(Double::doubleValue).toArray();
      String[] parts = key.split("_");
      SummaryStats s = SummaryStats.compute(array);
      s.setAlgorithm(parts[0]);
      s.setTrack(parts[1]);
      stats.put(key, s);
    }

    return stats;
  }

  private Set<String> extractTracks(List<ResultRow> results) {
    Set<String> tracks = new HashSet<>();
    for (ResultRow row : results) {
      tracks.add(row.getTrack());
    }
    return tracks;
  }

  private Set<String> extractAlgorithms(List<ResultRow> results) {
    Set<String> algorithms = new HashSet<>();
    for (ResultRow row : results) {
      algorithms.add(row.getAlgorithm());
    }
    return algorithms;
  }

  private void exportResultsTable(Map<String, SummaryStats> stats) throws IOException {
    Files.createDirectories(outputDir);
    Path tableFile = outputDir.resolve("results_table.csv");

    StringBuilder table = new StringBuilder();
    table.append("algorithm,track,mean,std,best,worst\n");

    for (SummaryStats s : stats.values()) {
      table.append(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f\n",
          s.getAlgorithm(), s.getTrack(), s.getMean(), s.getStd(),
          s.getBest(), s.getWorst()));
    }

    Files.write(tableFile, table.toString().getBytes(StandardCharsets.UTF_8));
  }
}
