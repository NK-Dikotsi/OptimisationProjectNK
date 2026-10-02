package za.ac.uj.racinglines.track;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads Track definitions from YAML configuration files.
 * Config format:
 * ```yaml
 * name: oval
 * type: oval
 * length: 200
 * width: 100
 * radius: 50
 * track_width: 10
 * gates: 400
 * ```
 */
@RequiredArgsConstructor
public class TrackLoader {
  private static final Logger logger = LoggerFactory.getLogger(TrackLoader.class);

  private final ObjectMapper yamlMapper;
  private final TrackFactory factory;

  /**
   * Load a track from a YAML file.
   */
  public Track loadFromYaml(Path filePath) throws IOException {
    Map<String, Object> config = yamlMapper.readValue(filePath.toFile(),
        new TypeReference<Map<String, Object>>() {});
    return createTrackFromConfig(config);
  }

  /**
   * Load a track from YAML content string.
   */
  public Track loadFromYamlString(String yaml) throws IOException {
    Map<String, Object> config = yamlMapper.readValue(yaml,
        new TypeReference<Map<String, Object>>() {});
    return createTrackFromConfig(config);
  }

  /**
   * Create a track from a configuration map.
   */
  private Track createTrackFromConfig(Map<String, Object> config) {
    String name = getStringOrThrow(config, "name", "Track name required");
    String type = getStringOrThrow(config, "type", "Track type required");
    double trackWidth = getDoubleOrThrow(config, "track_width", "Track width required");

    return switch (type.toLowerCase()) {
      case "circle" -> factory.circle(
          name,
          getDoubleOrThrow(config, "radius", "Circle radius required"),
          trackWidth,
          getIntOrDefault(config, "gates", 500));

      case "oval" -> factory.oval(
          name,
          getDoubleOrThrow(config, "length", "Oval length required"),
          getDoubleOrThrow(config, "width", "Oval width required"),
          getDoubleOrThrow(config, "radius", "Oval radius required"),
          trackWidth,
          getIntOrDefault(config, "gates", 400));

      case "hairpin" -> factory.hairpin(
          name,
          getDoubleOrThrow(config, "radius", "Hairpin radius required"),
          getDoubleOrThrow(config, "straight_length", "Hairpin straight length required"),
          trackWidth,
          getIntOrDefault(config, "gates", 300));

      case "chicane" -> factory.chicane(
          name,
          getDoubleOrThrow(config, "amplitude", "Chicane amplitude required"),
          getDoubleOrThrow(config, "wavelength", "Chicane wavelength required"),
          trackWidth,
          getIntOrDefault(config, "gates", 300));

      default -> throw new IllegalArgumentException("Unknown track type: " + type);
    };
  }

  private String getStringOrThrow(Map<String, Object> config, String key, String message) {
    Object value = config.get(key);
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
    return value.toString();
  }

  private double getDoubleOrThrow(Map<String, Object> config, String key, String message) {
    Object value = config.get(key);
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
    return ((Number) value).doubleValue();
  }

  private int getIntOrDefault(Map<String, Object> config, String key, int defaultValue) {
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    return ((Number) value).intValue();
  }
}
