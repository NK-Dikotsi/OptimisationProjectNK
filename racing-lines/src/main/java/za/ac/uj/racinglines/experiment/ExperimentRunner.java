package za.ac.uj.racinglines.experiment;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.ac.uj.racinglines.integration.OptimizationEngine;
import za.ac.uj.racinglines.integration.RaceTracks;
import za.ac.uj.racinglines.integration.RunOutcome;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Locale;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
public class ExperimentRunner {
  private static final Logger logger = LoggerFactory.getLogger(ExperimentRunner.class);

  private final ExperimentConfig config;
  private final int workers;
  private final Path batchFolder;
  private final String batchId;
  private final OptimizationEngine engine = new OptimizationEngine(
      new LapTimeSimulator(new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0)),
      OptimizationEngine.Params.defaults());
  private final String gitCommit = readGitCommit();

  public static ExperimentRunner create(ExperimentConfig config, int workers) throws IOException {
    config.validate();

    String batchId = generateBatchId();
    Path batchFolder = Paths.get("runs", "_batches", batchId);
    Files.createDirectories(batchFolder);

    return new ExperimentRunner(config, workers, batchFolder, batchId);
  }

  public void run() {
    long startTime = System.currentTimeMillis();
    List<Run> runs = generateRuns();
    int totalRuns = runs.size();

    logger.info("Batch start", kv("batch_id", batchId),
        kv("total_runs", totalRuns), kv("workers", workers));

    List<Run> completedRuns = new ArrayList<>();
    List<Run> failedRuns = new ArrayList<>();
    int skipped = 0;

    // Filter out already completed runs
    List<Run> runsToExecute = new ArrayList<>();
    for (Run run : runs) {
      if (isRunCompleted(run)) {
        completedRuns.add(run);
        logger.info("Run skipped", kv("run_id", run.getRunId()),
            kv("reason", "already completed"));
        skipped++;
      } else {
        runsToExecute.add(run);
      }
    }

    // Execute runs
    if (workers == 1) {
      for (Run run : runsToExecute) {
        try {
          executeRun(run);
          completedRuns.add(run);
        } catch (Exception e) {
          failedRuns.add(run);
          logger.error("Run failed", kv("run_id", run.getRunId()),
              kv("error", e.getMessage()));
        }
      }
    } else {
      executeRunsParallel(runsToExecute, completedRuns, failedRuns);
    }

    long duration = System.currentTimeMillis() - startTime;

    logger.info("Batch end", kv("completed", completedRuns.size()),
        kv("failed", failedRuns.size()), kv("skipped", skipped),
        kv("total", totalRuns), kv("duration_s", duration / 1000.0));

    // Write results CSV
    try {
      writeResultsCSV(completedRuns);
    } catch (IOException e) {
      logger.error("Failed to write results CSV", kv("error", e.getMessage()));
    }
  }

  private List<Run> generateRuns() {
    List<Run> runs = new ArrayList<>();
    int runIndex = 0;

    for (String algorithm : config.getAlgorithms()) {
      for (String track : config.getTracks()) {
        for (int seed : config.getSeeds()) {
          runIndex++;
          String runId = String.format("%05d_%s_%s_%d", runIndex, algorithm, track, seed);
          Path runFolder = batchFolder.resolve(runId);

          Random seedRandom = new Random(seed);
          int derivedSeed = seedRandom.nextInt();

          Run run = Run.builder()
              .runId(runId)
              .algorithm(algorithm)
              .track(track)
              .seed(derivedSeed)
              .budget(config.getBudget())
              .runFolder(runFolder)
              .createdAt(Instant.now())
              .status("pending")
              .build();

          runs.add(run);
        }
      }
    }

    return runs;
  }

  private void executeRun(Run run) throws IOException {
    Files.createDirectories(run.getRunFolder());
    run.setStatus("running");
    logger.info("run_started", kv("run_id", run.getRunId()), kv("algorithm", run.getAlgorithm()),
        kv("track", run.getTrack()), kv("seed", run.getSeed()));

    long start = System.nanoTime();
    try {
      RaceTracks.RaceTrack rt = RaceTracks.byName(run.getTrack());
      RunOutcome out = engine.optimize(run.getAlgorithm(), rt.name(), rt.track(), rt.width(),
          config.getBudget(), run.getSeed());
      long durationMs = (System.nanoTime() - start) / 1_000_000L;

      run.setBestLap(out.bestLap());
      run.setBaselineLap(out.baselineLap());
      run.setEvaluationsUsed(out.evaluationsUsed());
      run.setEvalsTo1Pct(out.evalsTo1Pct());
      run.setDuration(durationMs);
      run.setStatus("completed");
      run.setCompletedAt(Instant.now());

      writeConvergence(run.getRunFolder(), out);
      writeBestLine(run.getRunFolder(), out);

      logger.info("run_finished", kv("run_id", run.getRunId()),
          kv("algorithm", run.getAlgorithm()), kv("track", run.getTrack()),
          kv("seed", run.getSeed()), kv("best_lap", out.bestLap()),
          kv("baseline_lap", out.baselineLap()), kv("improvement_pct", out.improvementPct()),
          kv("evals_used", out.evaluationsUsed()), kv("duration_s", durationMs / 1000.0));
    } catch (RuntimeException e) {
      run.setStatus("failed");
      run.setFailureReason(e.toString());
      logger.error("run_failed", kv("run_id", run.getRunId()), kv("error", e.toString()), e);
      throw new IOException("Run " + run.getRunId() + " failed", e);
    }
  }

  private static void writeConvergence(Path runFolder, RunOutcome out) throws IOException {
    StringBuilder sb = new StringBuilder("evals,best_lap\n");
    for (RunOutcome.Point p : out.history()) {
      sb.append(String.format(Locale.ROOT, "%d,%.6f\n", p.evaluations(), p.bestLap()));
    }
    Files.writeString(runFolder.resolve("convergence.csv"), sb.toString(), StandardCharsets.UTF_8);
  }

  private static void writeBestLine(Path runFolder, RunOutcome out) throws IOException {
    StringBuilder sb = new StringBuilder("gate,offset_m\n");
    double[] offsets = out.bestOffsets();
    for (int i = 0; i < offsets.length; i++) {
      sb.append(String.format(Locale.ROOT, "%d,%.6f\n", i, offsets[i]));
    }
    Files.writeString(runFolder.resolve("best_line.csv"), sb.toString(), StandardCharsets.UTF_8);
  }

  private void executeRunsParallel(List<Run> runsToExecute, List<Run> completedRuns,
                                   List<Run> failedRuns) {
    ExecutorService executor = Executors.newFixedThreadPool(workers);

    for (Run run : runsToExecute) {
      executor.submit(() -> {
        try {
          executeRun(run);
          synchronized (completedRuns) {
            completedRuns.add(run);
          }
        } catch (Exception e) {
          synchronized (failedRuns) {
            failedRuns.add(run);
          }
        }
      });
    }

    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void writeResultsCSV(List<Run> completedRuns) throws IOException {
    Path resultsFile = batchFolder.resolve("results.csv");

    StringBuilder csv = new StringBuilder();
    csv.append("run_id,algorithm,track,seed,best_lap_s,baseline_lap_s,improvement_pct,")
        .append("evals_used,evals_to_1pct,duration_s,git_commit\n");

    for (Run run : completedRuns) {
      if (run.getBestLap() == null || run.getBaselineLap() == null) {
        throw new IllegalStateException("Run " + run.getRunId() + " has no result");
      }
      csv.append(String.format(Locale.ROOT, "%s,%s,%s,%d,%.4f,%.4f,%.3f,%d,%d,%.3f,%s\n",
          run.getRunId(),
          run.getAlgorithm(),
          run.getTrack(),
          run.getSeed(),
          run.getBestLap(),
          run.getBaselineLap(),
          run.getImprovementPct(),
          run.getEvaluationsUsed(),
          run.getEvalsTo1Pct(),
          run.getDuration() / 1000.0,
          gitCommit));
    }

    Files.writeString(resultsFile, csv.toString(), StandardCharsets.UTF_8);
  }

  private static String readGitCommit() {
    try {
      Process p = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
      String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (p.waitFor() == 0 && out.matches("[0-9a-f]{40}")) {
        return out;
      }
    } catch (IOException | InterruptedException e) {
      // fall through
    }
    LoggerFactory.getLogger(ExperimentRunner.class)
        .warn("git_commit_unavailable: run from inside the git repository");
    return "unknown";
  }

  private boolean isRunCompleted(Run run) {
    Path statusFile = run.getRunFolder().resolve("status.txt");
    return Files.exists(statusFile);
  }

  private static String generateBatchId() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
  }
}
