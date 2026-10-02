package za.ac.uj.racinglines.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GitInfo {

  public static String getCommitHash() {
    try {
      ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
      pb.redirectErrorStream(true);
      Process p = pb.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String commit = reader.readLine();
        p.waitFor();
        return commit != null ? commit.trim() : "unknown";
      }
    } catch (Exception e) {
      return "unknown";
    }
  }

  public static boolean isDirty() {
    try {
      ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
      pb.redirectErrorStream(true);
      Process p = pb.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line = reader.readLine();
        p.waitFor();
        return line != null && !line.trim().isEmpty();
      }
    } catch (Exception e) {
      return true;
    }
  }
}
