package za.ac.uj.racinglines.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;

import static org.assertj.core.api.Assertions.*;

class IntegrationTest {

  private final LapTimeSimulator simulator =
      new LapTimeSimulator(new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0));
  private final OptimizationEngine engine =
      new OptimizationEngine(simulator, OptimizationEngine.Params.defaults());

  // I1: ACO on oval track
  @Test
  @DisplayName("I1 ACO on oval")
  void testACOOnOval() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome result = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 500, 42);

    assertThat(result).isNotNull();
    assertThat(result.algorithm()).isEqualTo("aco");
    assertThat(result.track()).isEqualTo("oval");
    assertThat(result.bestOffsets()).hasSize(rt.track().gateCount());
    assertThat(result.bestLap()).isFinite().isGreaterThan(0);
    assertThat(result.evaluationsUsed()).isLessThanOrEqualTo(500);
  }

  // I2: PSO on hairpin track
  @Test
  @DisplayName("I2 PSO on hairpin")
  void testPSOOnHairpin() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("hairpin");
    RunOutcome result = engine.optimize("pso", rt.name(), rt.track(), rt.width(), 500, 42);

    assertThat(result).isNotNull();
    assertThat(result.algorithm()).isEqualTo("pso");
    assertThat(result.track()).isEqualTo("hairpin");
    assertThat(result.bestOffsets()).hasSize(rt.track().gateCount());
    assertThat(result.bestLap()).isFinite().isGreaterThan(0);
  }

  // I3: Adaptive evaporation on oval
  @Test
  @DisplayName("I3 adaptive on oval")
  void testAdaptiveEvaporationOnOval() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome result = engine.optimize("adaptive_evaporation", rt.name(), rt.track(), rt.width(), 500, 42);

    assertThat(result).isNotNull();
    assertThat(result.algorithm()).isEqualTo("adaptive_evaporation");
    assertThat(result.bestOffsets()).hasSize(rt.track().gateCount());
    assertThat(result.bestLap()).isFinite().isGreaterThan(0);
  }

  // I4: Improvement percentage
  @Test
  @DisplayName("I4 improvement percentage")
  void testImprovementPercentage() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome result = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 300, 42);

    double improvement = result.improvementPct();
    assertThat(improvement).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
  }

  // I5: LapTimeObjective evaluation
  @Test
  @DisplayName("I5 lap time objective")
  void testLapTimeObjective() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    LapTimeObjective objective = new LapTimeObjective(rt.track(), simulator, rt.width());

    assertThat(objective.getDimension()).isEqualTo(rt.track().gateCount());
    assertThat(objective.getLowerBounds()).hasSize(rt.track().gateCount());
    assertThat(objective.getUpperBounds()).hasSize(rt.track().gateCount());

    double[] centreline = new double[rt.track().gateCount()];
    double lap = objective.evaluate(centreline);
    assertThat(lap).isFinite().isGreaterThan(0);
  }

  // I6: NodeLapTimeObjective for ACO
  @Test
  @DisplayName("I6 node lap time objective")
  void testNodeLapTimeObjective() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("hairpin");
    LapTimeObjective lap = new LapTimeObjective(rt.track(), simulator, rt.width());
    NodeLapTimeObjective nodes = new NodeLapTimeObjective(lap, 11);

    assertThat(nodes.getNumGates()).isEqualTo(rt.track().gateCount());
    assertThat(nodes.getNumNodes()).isEqualTo(11);

    int[] centreline = new int[rt.track().gateCount()];
    for (int i = 0; i < centreline.length; i++) {
      centreline[i] = 5; // Middle node
    }

    double lapTime = nodes.evaluateLine(centreline);
    assertThat(lapTime).isFinite().isGreaterThan(0);
  }

  // I7: Deterministic results with same seed
  @Test
  @DisplayName("I7 deterministic with seed")
  void testDeterministicResults() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome result1 = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 300, 42);
    RunOutcome result2 = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 300, 42);

    assertThat(result1.bestLap()).isCloseTo(result2.bestLap(), within(1e-10));
  }

  // I8: Different seeds give different results
  @Test
  @DisplayName("I8 different seeds")
  void testDifferentSeeds() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");
    RunOutcome result1 = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 300, 42);
    RunOutcome result2 = engine.optimize("aco", rt.name(), rt.track(), rt.width(), 300, 99);

    // Results should be different (with very high probability)
    assertThat(result1.bestLap()).isNotCloseTo(result2.bestLap(), within(0.01));
  }

  // I9: All algorithms run without errors
  @Test
  @DisplayName("I9 all algorithms functional")
  void testAllAlgorithmsFunctional() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");

    assertThatNoException()
        .isThrownBy(() -> engine.optimize("aco", rt.name(), rt.track(), rt.width(), 200, 1));
    assertThatNoException()
        .isThrownBy(() -> engine.optimize("pso", rt.name(), rt.track(), rt.width(), 200, 1));
    assertThatNoException().isThrownBy(
        () -> engine.optimize("adaptive_evaporation", rt.name(), rt.track(), rt.width(), 200, 1));
  }

  // I10: Invalid algorithm throws error
  @Test
  @DisplayName("I10 invalid algorithm")
  void testInvalidAlgorithm() {
    RaceTracks.RaceTrack rt = RaceTracks.byName("oval");

    assertThatThrownBy(
        () -> engine.optimize("invalid", rt.name(), rt.track(), rt.width(), 200, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
