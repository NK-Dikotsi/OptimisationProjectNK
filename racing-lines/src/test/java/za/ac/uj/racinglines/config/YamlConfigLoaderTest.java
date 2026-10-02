package za.ac.uj.racinglines.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import za.ac.uj.racinglines.experiment.ExperimentConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class YamlConfigLoaderTest {

  // Y1: Load valid config
  @Test
  @DisplayName("Y1 load valid config")
  void testLoadValidConfig(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: test_seed
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
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configFile);

    assertThat(config).isNotNull();
    assertThat(config.getBudget()).isEqualTo(1000);
    assertThat(config.getAlgorithms()).contains("aco", "pso");
    assertThat(config.getTracks()).contains("oval");
    assertThat(config.getSeeds()).contains(42, 43);
  }

  // Y2: Multiple algorithms
  @Test
  @DisplayName("Y2 multiple algorithms")
  void testMultipleAlgorithms(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seed123
          budget: 5000
        algorithms:
          - aco
          - pso
          - adaptive_evaporation
        tracks:
          - oval
        seeds:
          - 1
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configFile);

    assertThat(config.getAlgorithms()).hasSize(3);
    assertThat(config.getAlgorithms()).contains("aco", "pso", "adaptive_evaporation");
  }

  // Y3: Multiple tracks
  @Test
  @DisplayName("Y3 multiple tracks")
  void testMultipleTracks(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seed456
          budget: 2000
        algorithms:
          - aco
        tracks:
          - oval
          - tight
          - road
        seeds:
          - 42
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configFile);

    assertThat(config.getTracks()).hasSize(3);
    assertThat(config.getTracks()).contains("oval", "tight", "road");
  }

  // Y4: Multiple seeds
  @Test
  @DisplayName("Y4 multiple seeds")
  void testMultipleSeeds(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seedabc
          budget: 1500
        algorithms:
          - aco
        tracks:
          - oval
        seeds:
          - 42
          - 43
          - 44
          - 45
          - 46
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configFile);

    assertThat(config.getSeeds()).hasSize(5);
    assertThat(config.getSeeds()).contains(42, 43, 44, 45, 46);
  }

  // Y5: File not found error
  @Test
  @DisplayName("Y5 file not found")
  void testFileNotFound() {
    Path nonExistent = Path.of("nonexistent.yaml");

    assertThatThrownBy(() -> YamlConfigLoader.loadExperimentConfig(nonExistent))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("not found");
  }

  // Y6: Invalid YAML
  @Test
  @DisplayName("Y6 invalid yaml")
  void testInvalidYaml(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        [invalid: yaml: content
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    // Should throw or handle gracefully
    assertThatThrownBy(() -> YamlConfigLoader.loadExperimentConfig(configFile))
        .isInstanceOf(IOException.class);
  }

  // Y7: Missing algorithms
  @Test
  @DisplayName("Y7 missing algorithms")
  void testMissingAlgorithms(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seed789
          budget: 1000
        tracks:
          - oval
        seeds:
          - 42
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> YamlConfigLoader.loadExperimentConfig(configFile))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("algorithms");
  }

  // Y8: Missing tracks
  @Test
  @DisplayName("Y8 missing tracks")
  void testMissingTracks(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seedxyz
          budget: 1000
        algorithms:
          - aco
        seeds:
          - 42
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> YamlConfigLoader.loadExperimentConfig(configFile))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("tracks");
  }

  // Y9: Missing seeds
  @Test
  @DisplayName("Y9 missing seeds")
  void testMissingSeeds(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seeddef
          budget: 1000
        algorithms:
          - aco
        tracks:
          - oval
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> YamlConfigLoader.loadExperimentConfig(configFile))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("seeds");
  }

  // Y10: Budget parameter
  @Test
  @DisplayName("Y10 budget parameter")
  void testBudgetParameter(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.yaml");
    String yaml = """
        experiment:
          base_seed: seed999
          budget: 50000
        algorithms:
          - aco
        tracks:
          - oval
        seeds:
          - 42
        """;
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    ExperimentConfig config = YamlConfigLoader.loadExperimentConfig(configFile);

    assertThat(config.getBudget()).isEqualTo(50000);
  }
}
