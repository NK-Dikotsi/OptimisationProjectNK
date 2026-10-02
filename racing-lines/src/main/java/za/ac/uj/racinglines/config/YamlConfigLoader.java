package za.ac.uj.racinglines.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import za.ac.uj.racinglines.experiment.ExperimentConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlConfigLoader {

  public static ExperimentConfig loadExperimentConfig(Path configPath) throws IOException {
    if (!Files.exists(configPath)) {
      throw new IOException("Config file not found: " + configPath);
    }

    String content = Files.readString(configPath);
    Yaml yaml = new Yaml();
    Map<String, Object> data;

    try {
      data = yaml.load(content);
    } catch (YAMLException e) {
      throw new IOException("Invalid YAML file: " + e.getMessage(), e);
    }

    if (data == null) {
      throw new IOException("Invalid YAML file: " + configPath);
    }

    // Extract experiment section
    Map<String, Object> experiment = getMap(data, "experiment");
    String baseSeed = getString(experiment, "base_seed", "default_seed");
    long budget = getLong(experiment, "budget", 1000L);

    // Extract algorithms list
    List<String> algorithms = getStringList(data, "algorithms");
    if (algorithms.isEmpty()) {
      throw new IOException("No algorithms specified in config");
    }

    // Extract tracks list
    List<String> tracks = getStringList(data, "tracks");
    if (tracks.isEmpty()) {
      throw new IOException("No tracks specified in config");
    }

    // Extract seeds list
    List<Integer> seeds = getIntList(data, "seeds");
    if (seeds.isEmpty()) {
      throw new IOException("No seeds specified in config");
    }

    return new ExperimentConfig(baseSeed, budget, algorithms, tracks, seeds);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getMap(Map<String, Object> data, String key) {
    Object value = data.get(key);
    if (value instanceof Map) {
      return (Map<String, Object>) value;
    }
    return new java.util.HashMap<>();
  }

  private static String getString(Map<String, Object> data, String key, String defaultValue) {
    Object value = data.get(key);
    if (value instanceof String) {
      return (String) value;
    }
    return defaultValue;
  }

  private static long getLong(Map<String, Object> data, String key, long defaultValue) {
    Object value = data.get(key);
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else if (value instanceof Long) {
      return (Long) value;
    }
    return defaultValue;
  }

  @SuppressWarnings("unchecked")
  private static List<String> getStringList(Map<String, Object> data, String key) {
    List<String> result = new ArrayList<>();
    Object value = data.get(key);

    if (value instanceof List) {
      List<Object> list = (List<Object>) value;
      for (Object item : list) {
        if (item instanceof String) {
          result.add((String) item);
        }
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> getIntList(Map<String, Object> data, String key) {
    List<Integer> result = new ArrayList<>();
    Object value = data.get(key);

    if (value instanceof List) {
      List<Object> list = (List<Object>) value;
      for (Object item : list) {
        if (item instanceof Integer) {
          result.add((Integer) item);
        } else if (item instanceof String) {
          try {
            result.add(Integer.parseInt((String) item));
          } catch (NumberFormatException e) {
            // Skip invalid numbers
          }
        }
      }
    }

    return result;
  }
}
