package za.ac.uj.racinglines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.ac.uj.racinglines.analysis.ResultsAnalyzer;
import za.ac.uj.racinglines.config.YamlConfigLoader;
import za.ac.uj.racinglines.experiment.ExperimentConfig;
import za.ac.uj.racinglines.experiment.ExperimentRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    try {
      if (args.length == 0) {
        printHelp();
        System.exit(1);
      }

      String command = args[0];

      switch (command) {
        case "experiment":
          handleExperiment(args);
          break;
        case "analysis":
          handleAnalysis(args);
          break;
        case "help":
          printHelp();
          break;
        default:
          System.err.println("Unknown command: " + command);
          printHelp();
          System.exit(1);
      }
    } catch (Exception e) {
      logger.error("Fatal error", e);
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void handleExperiment(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: experiment <config.yaml> [--workers N]");
      System.exit(1);
    }

    Path configPath = Paths.get(args[1]);
    if (!Files.exists(configPath)) {
      throw new IOException("Config file not found: " + configPath);
    }

    int workers = 1;
    for (int i = 2; i < args.length; i++) {
      if ("--workers".equals(args[i]) && i + 1 < args.length) {
        workers = Integer.parseInt(args[i + 1]);
      }
    }

    // Parse YAML configuration
    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configPath);
    config.validate();

    logger.info("Starting experiment with config: {}", configPath);
    logger.info("Workers: {}, Algorithms: {}, Tracks: {}, Seeds: {}",
        workers, config.getAlgorithms().size(), config.getTracks().size(), config.getSeeds().size());

    ExperimentRunner runner = ExperimentRunner.create(config, workers);
    runner.run();

    logger.info("Experiment completed successfully");
  }

  private static void handleAnalysis(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: analysis <batch_folder> [--out output_dir]");
      System.exit(1);
    }

    Path batchPath = Paths.get(args[1]);
    if (!Files.exists(batchPath)) {
      throw new IOException("Batch folder not found: " + batchPath);
    }

    Path outputDir = Paths.get("paper/figures");
    for (int i = 2; i < args.length; i++) {
      if ("--out".equals(args[i]) && i + 1 < args.length) {
        outputDir = Paths.get(args[i + 1]);
      }
    }

    logger.info("Starting analysis from batch: {}", batchPath);
    logger.info("Output directory: {}", outputDir);

    ResultsAnalyzer analyzer = new ResultsAnalyzer(batchPath, outputDir);
    analyzer.analyze();

    logger.info("Analysis completed successfully");
    logger.info("Results written to: {}", outputDir);
  }


  private static void printHelp() {
    System.out.println("Racing Lines Optimization");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  experiment <config.yaml> [--workers N]");
    System.out.println("    Run optimization experiments from configuration");
    System.out.println("    --workers N   Number of parallel workers (default: 1)");
    System.out.println();
    System.out.println("  analysis <batch_folder> [--out output_dir]");
    System.out.println("    Analyze and visualize results from a batch");
    System.out.println("    --out dir     Output directory for figures (default: paper/figures)");
    System.out.println();
    System.out.println("  help");
    System.out.println("    Show this help message");
  }
}
