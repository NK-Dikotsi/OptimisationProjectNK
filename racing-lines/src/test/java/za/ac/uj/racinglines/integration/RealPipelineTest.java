package za.ac.uj.racinglines.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;

/** Tests that would have caught the placeholder results: the pipeline must use real physics. */
class RealPipelineTest {

  // Same physics values as ExperimentRunner. Order: mu, g, v_top, a_acc, a_brake
  private final LapTimeSimulator simulator =
      new LapTimeSimulator(new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0));
  private final OptimizationEngine engine =
      new OptimizationEngine(simulator, OptimizationEngine.Params.defaults());

  private static final List<String> TRACKS = List.of("oval", "hairpin", "chicane");

  @Test
  @DisplayName("R1 centreline baselines differ between tracks")
  void baselinesDiffer() {
    Set<Long> rounded = new HashSet<>();
    for (String name : TRACKS) {
      RaceTracks.RaceTrack rt = RaceTracks.byName(name);
      double baseline = new LapTimeObjective(rt.track(), simulator, rt.width()).baseline();
      System.out.printf("Centreline lap %-8s %.3f s%n", name, baseline);
      rounded.add(Math.round(baseline * 1000));
    }
    assertThat(rounded).hasSize(TRACKS.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"pso", "aco", "adaptive_evaporation"})
  @DisplayName("R2 every algorithm reports a real, verified lap no worse than the centreline")
  void bestNoWorseThanBaseline(String algorithm) {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome out = engine.optimize(algorithm, rt.name(), rt.track(), rt.width(), 1_000, 42);

    assertThat(out.bestLap()).isLessThanOrEqualTo(out.baselineLap() + 1e-9);
    assertThat(out.evaluationsUsed()).isPositive().isLessThanOrEqualTo(1_000);
    assertThat(out.evalsTo1Pct()).isPositive().isLessThanOrEqualTo(out.evaluationsUsed());
    assertThat(out.history()).isNotEmpty();
    // Best-so-far never gets worse along the history.
    for (int i = 1; i < out.history().size(); i++) {
      assertThat(out.history().get(i).bestLap())
          .isLessThanOrEqualTo(out.history().get(i - 1).bestLap() + 1e-12);
    }
  }

  @Test
  @DisplayName("R3 same seed gives an identical result")
  void reproducible() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("hairpin");
    RunOutcome a = engine.optimize("pso", rt.name(), rt.track(), rt.width(), 1_000, 7);
    RunOutcome b = engine.optimize("pso", rt.name(), rt.track(), rt.width(), 1_000, 7);
    assertThat(a.bestLap()).isEqualTo(b.bestLap());
    assertThat(a.bestOffsets()).containsExactly(b.bestOffsets());
  }

  @Test
  @Tag("slow")
  @DisplayName("R4 more budget gives better laps on average")
  void moreBudgetHelps() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("hairpin");
    double small = 0;
    double large = 0;
    for (long seed = 1; seed <= 3; seed++) {
      small += engine.optimize("pso", rt.name(), rt.track(), rt.width(), 300, seed).bestLap();
      large += engine.optimize("pso", rt.name(), rt.track(), rt.width(), 5_000, seed).bestLap();
    }
    assertThat(large / 3).isLessThan(small / 3);
  }

  @Test
  @DisplayName("R5 evals_to_1pct is the first point within 1% of the final best")
  void evalsTo1Pct() {
    List<RunOutcome.Point> h = List.of(
        new RunOutcome.Point(100, 60.0),
        new RunOutcome.Point(200, 55.0),
        new RunOutcome.Point(300, 50.4),
        new RunOutcome.Point(400, 50.0));
    assertThat(RunOutcome.evalsTo1Pct(h, 50.0)).isEqualTo(300);
  }

  @Test
  @DisplayName("R6 ACO node mapping spans the track and includes the centreline")
  void nodeMapping() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    LapTimeObjective lap = new LapTimeObjective(rt.track(), simulator, rt.width());
    NodeLapTimeObjective nodes = new NodeLapTimeObjective(lap, 11);
    assertThat(nodes.nodeOffset(0)).isCloseTo(-rt.width() / 2, within(1e-12));
    assertThat(nodes.nodeOffset(10)).isCloseTo(rt.width() / 2, within(1e-12));
    assertThat(nodes.nodeOffset(5)).isCloseTo(0.0, within(1e-12));
  }
}
