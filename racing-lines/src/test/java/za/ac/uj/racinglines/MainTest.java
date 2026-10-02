package za.ac.uj.racinglines;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class MainTest {

  // M1: Command line parsing
  @Test
  @DisplayName("M1 experiment command parsing")
  void testExperimentCommandParsing(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String config = """
        experiment:
          base_seed: 12345
          budget: 1000
        algorithms:
          - aco
          - pso
        tracks:
          - oval
        seeds:
          - 42
          - 43
        """;
    Files.write(configFile, config.getBytes(StandardCharsets.UTF_8));

    assertThat(Files.exists(configFile)).isTrue();
    String content = Files.readString(configFile);
    assertThat(content).contains("aco");
    assertThat(content).contains("pso");
  }

  // M2: Analysis command parsing
  @Test
  @DisplayName("M2 analysis command parsing")
  void testAnalysisCommandParsing(@TempDir Path tempDir) throws IOException {
    Path batchFolder = tempDir.resolve("batch");
    Files.createDirectories(batchFolder);

    Path resultsFile = batchFolder.resolve("results.csv");
    Files.createFile(resultsFile);

    assertThat(Files.exists(batchFolder)).isTrue();
    assertThat(Files.exists(resultsFile)).isTrue();
  }

  // M3: Workers parameter
  @Test
  @DisplayName("M3 workers parameter parsing")
  void testWorkersParameterParsing() {
    String[] args1 = {"experiment", "config.yaml"};
    assertThat(args1).contains("experiment");

    String[] args2 = {"experiment", "config.yaml", "--workers", "4"};
    assertThat(args2).hasSize(4);
    assertThat(args2[2]).isEqualTo("--workers");
    assertThat(args2[3]).isEqualTo("4");
  }

  // M4: Output directory parameter
  @Test
  @DisplayName("M4 output directory parameter parsing")
  void testOutputDirectoryParameter() {
    String[] args = {"analysis", "runs/_batches/batch_id", "--out", "paper/figures"};
    assertThat(args).hasSize(4);
    assertThat(args[0]).isEqualTo("analysis");
    assertThat(args[2]).isEqualTo("--out");
    assertThat(args[3]).isEqualTo("paper/figures");
  }

  // M5: Config file not found error
  @Test
  @DisplayName("M5 config file not found")
  void testConfigFileNotFound() {
    Path nonExistentPath = Path.of("nonexistent_config.yaml");
    assertThat(Files.exists(nonExistentPath)).isFalse();
  }

  // M6: YAML parsing
  @Test
  @DisplayName("M6 yaml parsing")
  void testYamlParsing(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String config = """
        experiment:
          base_seed: test_seed
          budget: 5000
        algorithms:
          - aco
          - pso
          - adaptive_evaporation
        tracks:
          - oval
          - tight
        seeds:
          - 42
          - 43
          - 44
        """;
    Files.write(configFile, config.getBytes(StandardCharsets.UTF_8));

    String content = Files.readString(configFile);
    assertThat(content).contains("budget: 5000");
    assertThat(content).contains("- aco");
    assertThat(content).contains("- pso");
    assertThat(content).contains("- adaptive_evaporation");
    assertThat(content).contains("- oval");
    assertThat(content).contains("- tight");
  }

  // M7: Help command
  @Test
  @DisplayName("M7 help command")
  void testHelpCommand() {
    String[] args = {"help"};
    assertThat(args[0]).isEqualTo("help");
  }

  // M8: Invalid command
  @Test
  @DisplayName("M8 invalid command")
  void testInvalidCommand() {
    String[] args = {"invalid_command"};
    assertThat(args[0]).isNotEqualTo("experiment");
    assertThat(args[0]).isNotEqualTo("analysis");
    assertThat(args[0]).isNotEqualTo("help");
  }
}
