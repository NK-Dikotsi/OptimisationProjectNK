package za.ac.uj.racinglines.analysis;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class PlotGenerator {
  private final Path outputDir;

  public void generateTrackPlots(String track, java.util.List<String> algorithms) throws IOException {
    Path plotFile = outputDir.resolve(String.format("track_%s.pdf", track));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public void generateSpeedProfile(String track, String algorithm) throws IOException {
    Path plotFile = outputDir.resolve(String.format("speed_%s_%s.pdf", track, algorithm));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public void generateConvergenceCurves(String track, java.util.List<String> algorithms) throws IOException {
    Path plotFile = outputDir.resolve(String.format("convergence_%s.pdf", track));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public void generateDiversityCurves(String track, java.util.List<String> algorithms) throws IOException {
    Path plotFile = outputDir.resolve(String.format("diversity_%s.pdf", track));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public void generateBoxPlots(String track, java.util.List<String> algorithms) throws IOException {
    Path plotFile = outputDir.resolve(String.format("boxplot_%s.pdf", track));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public void generateSensitivityPlots(String parameter) throws IOException {
    Path plotFile = outputDir.resolve(String.format("sensitivity_%s.pdf", parameter));
    Files.createDirectories(outputDir);
    Files.write(plotFile, new byte[0]);
  }

  public boolean verifyPlotExists(String plotName) {
    Path plotFile = outputDir.resolve(plotName);
    return Files.exists(plotFile) && isNonEmpty(plotFile);
  }

  private boolean isNonEmpty(Path file) {
    try {
      return Files.size(file) > 0;
    } catch (IOException e) {
      return false;
    }
  }
}
