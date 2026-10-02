package za.ac.uj.racinglines.track;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * Serialization format for Track and offset vectors as CSV/JSON.
 * Stores the track name, centreline points, normals, width, and offset vector.
 */
@RequiredArgsConstructor
public class TrackCsvFormat {

  private final ObjectMapper mapper;

  /**
   * Serialize a Track and offset vector to a JSON string for storage.
   */
  public String toCSV(Track track, double[] offsets) {
    try {
      ObjectNode root = mapper.createObjectNode();

      root.put("name", track.getName());
      root.put("nGates", track.gateCount());
      root.put("width", track.getWidth());
      root.put("centrelineLength", track.getCentrelineLength());

      // Store centreline points
      ArrayNode centrelineArray = mapper.createArrayNode();
      for (double[] point : track.getCentrelinePoints()) {
        ArrayNode pointArray = mapper.createArrayNode();
        pointArray.add(point[0]);
        pointArray.add(point[1]);
        centrelineArray.add(pointArray);
      }
      root.set("centreline", centrelineArray);

      // Store normals
      ArrayNode normalsArray = mapper.createArrayNode();
      for (double[] normal : track.getNormals()) {
        ArrayNode normalArray = mapper.createArrayNode();
        normalArray.add(normal[0]);
        normalArray.add(normal[1]);
        normalsArray.add(normalArray);
      }
      root.set("normals", normalsArray);

      // Store offsets
      ArrayNode offsetsArray = mapper.createArrayNode();
      for (double offset : offsets) {
        offsetsArray.add(offset);
      }
      root.set("offsets", offsetsArray);

      return mapper.writeValueAsString(root);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize track", e);
    }
  }

  /**
   * Deserialize a Track from JSON.
   */
  public Track fromCSV(String json) {
    try {
      ObjectNode root = (ObjectNode) mapper.readTree(json);

      String name = root.get("name").asText();
      double width = root.get("width").asDouble();

      // Load centreline
      ArrayNode centrelineArray = (ArrayNode) root.get("centreline");
      double[][] centreline = new double[centrelineArray.size()][2];
      for (int i = 0; i < centrelineArray.size(); i++) {
        ArrayNode point = (ArrayNode) centrelineArray.get(i);
        centreline[i][0] = point.get(0).asDouble();
        centreline[i][1] = point.get(1).asDouble();
      }

      return Track.fromCentreline(name, centreline, width);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize track", e);
    }
  }

  /**
   * Extract the offset vector from a JSON string.
   */
  public double[] extractOffsets(String json) {
    try {
      ObjectNode root = (ObjectNode) mapper.readTree(json);
      ArrayNode offsetsArray = (ArrayNode) root.get("offsets");

      double[] offsets = new double[offsetsArray.size()];
      for (int i = 0; i < offsetsArray.size(); i++) {
        offsets[i] = offsetsArray.get(i).asDouble();
      }
      return offsets;
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract offsets", e);
    }
  }
}
