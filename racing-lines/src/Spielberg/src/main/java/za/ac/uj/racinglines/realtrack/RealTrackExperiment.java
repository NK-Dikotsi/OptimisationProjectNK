package za.ac.uj.racinglines.realtrack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import za.ac.uj.racinglines.integration.OptimizationEngine;
import za.ac.uj.racinglines.integration.RaceTracks;
import za.ac.uj.racinglines.integration.RunOutcome;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;
import za.ac.uj.racinglines.track.Track;

/**
 * Separate demonstration on one real circuit. It reuses the main pipeline unchanged
 * (same physics, same engine, same algorithms); only the control-point count is scaled
 * to the track length so control points stay about 25 m apart, as on the main tracks.
 *
 * <pre>
 * java -cp target/racing-lines.jar za.ac.uj.racinglines.realtrack.RealTrackExperiment \
 *     data/Spielberg.csv Spielberg 100000 3 data/Spielberg_raceline.csv
 * </pre>
 * Arguments: track csv, name, budget per run, number of seeds (42, 43, ...), optional
 * reference race line csv.
 */
public final class RealTrackExperiment {

  /** Same physics as the main experiment: mu, g, v_top, a_acc, a_brake. */
  static final PhysicsConfig PHYSICS = new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0);

  /** Target spacing between control points, as on the main tracks (20 points on ~500 m). */
  static final double CONTROL_SPACING_M = 25.0;

  static final String[] ALGORITHMS = {"pso", "aco"};

  private RealTrackExperiment() {}

  public static void main(String[] args) throws IOException {
    if (args.length < 4) {
      System.err.println("Usage: RealTrackExperiment <track.csv> <name> <budget> <seeds> [raceline.csv]");
      System.exit(2);
    }
    Path csv = Paths.get(args[0]);
    String name = args[1];
    long budget = Long.parseLong(args[2]);
    int seeds = Integer.parseInt(args[3]);
    Path raceline = args.length > 4 ? Paths.get(args[4]) : null;

    TumTrack tum = TumTrack.load(name, csv);
    Track track = tum.toTrack();
    double usableWidth = tum.trackWidth() - RaceTracks.CAR_WIDTH;
    if (usableWidth <= 0) {
      throw new IllegalStateException("Track narrower than the car: " + tum.trackWidth());
    }
    int controlPoints = controlPointsFor(tum.lengthMetres(), track.gateCount());

    OptimizationEngine.Params d = OptimizationEngine.Params.defaults();
    OptimizationEngine.Params params = new OptimizationEngine.Params(d.population(), d.acoNodes(),
        d.alpha(), d.beta(), d.rho(), d.rhoMin(), d.rhoMax(), d.inertia(), d.c1(), d.c2(),
        d.vmaxFractionOfWidth(), controlPoints);
    LapTimeSimulator simulator = new LapTimeSimulator(PHYSICS);
    OptimizationEngine engine = new OptimizationEngine(simulator, params);

    Path out = Paths.get("runs", "realtrack",
        name + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    Files.createDirectories(out);

    StringBuilder info = new StringBuilder();
    info.append(String.format(Locale.ROOT,
        "track=%s%nlength_m=%.1f%ngates=%d%nmin_half_width_m=%.3f%nmedian_half_width_m=%.3f%n"
            + "model_track_width_m=%.3f%ncar_width_m=%.1f%nusable_width_m=%.3f%n"
            + "control_points=%d%nbudget=%d%nseeds=%d%n",
        name, tum.lengthMetres(), track.gateCount(), tum.minHalfWidth(), tum.medianHalfWidth(),
        tum.trackWidth(), RaceTracks.CAR_WIDTH, usableWidth, controlPoints, budget, seeds));
    if (raceline != null) {
      double ref = simulator.lapTime(TumTrack.loadPolyline(raceline));
      info.append(String.format(Locale.ROOT,
          "reference_raceline_lap_s=%.4f (TUM minimum-curvature line, uses the full real width)%n",
          ref));
    }
    Files.writeString(out.resolve("info.txt"), info.toString(), StandardCharsets.UTF_8);
    System.out.print(info);

    StringBuilder results = new StringBuilder(
        "algorithm,seed,best_lap_s,baseline_lap_s,improvement_pct,evals_used,evals_to_1pct,duration_s\n");
    for (String algorithm : ALGORITHMS) {
      for (int s = 0; s < seeds; s++) {
        long seed = 42 + s;
        long start = System.nanoTime();
        RunOutcome r = engine.optimize(algorithm, name, track, usableWidth, budget, seed);
        double seconds = (System.nanoTime() - start) / 1e9;
        results.append(String.format(Locale.ROOT, "%s,%d,%.4f,%.4f,%.3f,%d,%d,%.2f%n",
            algorithm, seed, r.bestLap(), r.baselineLap(), r.improvementPct(),
            r.evaluationsUsed(), r.evalsTo1Pct(), seconds));
        writeRun(out.resolve(algorithm + "_" + seed), r);
        System.out.printf(Locale.ROOT, "%s seed %d: %.3f s (centreline %.3f s, %.2f%%) in %.1f s%n",
            algorithm, seed, r.bestLap(), r.baselineLap(), r.improvementPct(), seconds);
        Files.writeString(out.resolve("results.csv"), results.toString(), StandardCharsets.UTF_8);
      }
    }
    System.out.println("Results written to " + out.toAbsolutePath());
  }

  /** About one control point every 25 m, at least 4 and at most one per gate. */
  static int controlPointsFor(double lengthMetres, int gates) {
    int k = (int) Math.round(lengthMetres / CONTROL_SPACING_M);
    return Math.max(4, Math.min(gates, k));
  }

  private static void writeRun(Path folder, RunOutcome r) throws IOException {
    Files.createDirectories(folder);
    StringBuilder line = new StringBuilder("gate,offset_m\n");
    double[] o = r.bestOffsets();
    for (int i = 0; i < o.length; i++) {
      line.append(String.format(Locale.ROOT, "%d,%.6f%n", i, o[i]));
    }
    Files.writeString(folder.resolve("best_line.csv"), line.toString(), StandardCharsets.UTF_8);
    StringBuilder conv = new StringBuilder("evals,best_lap\n");
    for (RunOutcome.Point p : r.history()) {
      conv.append(String.format(Locale.ROOT, "%d,%.6f%n", p.evaluations(), p.bestLap()));
    }
    Files.writeString(folder.resolve("convergence.csv"), conv.toString(), StandardCharsets.UTF_8);
  }
}
