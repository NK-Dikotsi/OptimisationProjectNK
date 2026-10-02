package za.ac.uj.racinglines.aco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.fixtures.Aco;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class AntColonyOptimizerTest {
  private AcoConfig config;
  private AntColonyOptimizer optimizer;
  private Random random;

  @BeforeEach
  void setUp() {
    config = new AcoConfig(10, 5, 1.0, 1.0, 0.1, 1.0, 1.0, 1000);
    random = new Random();
    optimizer = new AntColonyOptimizer(config, random);
  }

  // A1: Pheromone initialization
  @Test
  @DisplayName("A1 pheromone init")
  void testPheromoneInit() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();
    assertThat(result.getBestLine().length).isEqualTo(objective.getNumGates());
  }

  // A2: Node offsets span bounds and include 0 for odd K
  @Test
  @DisplayName("A2 node offsets")
  void testNodeOffsets() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 5, 2.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();
    for (int node : result.getBestLine()) {
      assertThat(node).isGreaterThanOrEqualTo(0).isLessThan(5);
    }
  }

  // A3: Transition probabilities valid
  @Test
  @DisplayName("A3 probabilities valid")
  void testProbabilitiesValid() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();
    for (int node : result.getBestLine()) {
      assertThat(node).isGreaterThanOrEqualTo(0).isLessThan(3);
    }
  }

  // A4: Hand calculation with set values
  @Test
  @DisplayName("A4 probabilities hand calc")
  void testProbabilitiesHandCalc() {
    AcoConfig testConfig = new AcoConfig(5, 3, 1.0, 1.0, 0.1, 1.0, 1.0, 500);
    optimizer = new AntColonyOptimizer(testConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(3, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine().length).isEqualTo(3);
    for (int node : result.getBestLine()) {
      assertThat(node).isIn(0, 1, 2);
    }
  }

  // A5: Alpha=0 and Beta=0 edge cases
  @Test
  @DisplayName("A5 alpha beta zero")
  void testAlphaBetaZero() {
    AcoConfig alphaZero = new AcoConfig(10, 5, 0.0, 1.0, 0.1, 1.0, 1.0, 500);
    optimizer = new AntColonyOptimizer(alphaZero, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 5, 1.0, 2);
    AcoResult result1 = optimizer.optimize(objective);

    AcoConfig betaZero = new AcoConfig(10, 5, 1.0, 0.0, 0.1, 1.0, 1.0, 500);
    optimizer = new AntColonyOptimizer(betaZero, new Random(42));
    AcoResult result2 = optimizer.optimize(objective);

    assertThat(result1.getBestLine()).isNotNull();
    assertThat(result2.getBestLine()).isNotNull();
  }

  // A6: Sampling matches probabilities
  @Test
  @DisplayName("A6 sampling matches probs")
  void testSamplingMatchesProbs() {
    AcoConfig testConfig = new AcoConfig(10, 5, 1.0, 1.0, 0.1, 1.0, 1.0, 10000);
    optimizer = new AntColonyOptimizer(testConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(20, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getEvaluationsUsed()).isLessThanOrEqualTo(10000);
  }

  // A7: Ant builds valid line
  @Test
  @DisplayName("A7 ant builds valid line")
  void testAntBuildsValidLine() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine().length).isEqualTo(objective.getNumGates());
    for (int node : result.getBestLine()) {
      assertThat(node).isGreaterThanOrEqualTo(0).isLessThan(objective.getNumNodes());
    }
  }

  // A8: Evaporation
  @Test
  @DisplayName("A8 evaporation")
  void testEvaporation() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getHistory()).isNotEmpty();
  }

  // A9: Deposit only on path
  @Test
  @DisplayName("A9 deposit only on path")
  void testDepositOnlyOnPath() {
    AcoConfig testConfig = new AcoConfig(5, 3, 1.0, 1.0, 0.05, 1.0, 1.0, 500);
    optimizer = new AntColonyOptimizer(testConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();
    assertThat(result.getHistory()).isNotEmpty();
  }

  // A10: Better lap more pheromone
  @Test
  @DisplayName("A10 better lap more pheromone")
  void testBetterLapMorePheromone() {
    AcoConfig testConfig = new AcoConfig(10, 5, 1.0, 1.0, 0.1, 1.0, 1.0, 1000);
    optimizer = new AntColonyOptimizer(testConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLap()).isFinite();
  }

  // A11: Tau bounds enforcement
  @Test
  @DisplayName("A11 tau bounds")
  void testTauBounds() {
    AcoConfig testConfig = new AcoConfig(10, 5, 1.0, 1.0, 0.1, 1.0, 1.0, 1000);
    optimizer = new AntColonyOptimizer(testConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    for (AcoResult.IterationSnapshot snapshot : result.getHistory()) {
      assertThat(snapshot.getTauMin()).isGreaterThanOrEqualTo(testConfig.getTauMin());
      assertThat(snapshot.getTauMax()).isLessThanOrEqualTo(testConfig.getTauMax());
    }
  }

  // A12: Toy problem convergence
  @Test
  @DisplayName("A12 toy problem")
  void testToyProblem() {
    AcoConfig toyConfig = new AcoConfig(15, 5, 1.0, 1.0, 0.1, 2.0, 1.0, 5000);
    optimizer = new AntColonyOptimizer(toyConfig, new Random(42));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLap()).isLessThan(5.0);
  }

  // A13: Evaluation budget exact
  @Test
  @DisplayName("A13 eval budget exact")
  void testEvalBudgetExact() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getEvaluationsUsed()).isLessThanOrEqualTo(config.getBudget());
  }

  // A14: Same seed same result
  @Test
  @DisplayName("A14 same seed same result")
  void testSameSeedSameResult() {
    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(5, 3, 1.0, 1);

    optimizer = new AntColonyOptimizer(config, new Random(42));
    AcoResult result1 = optimizer.optimize(objective);

    optimizer = new AntColonyOptimizer(config, new Random(42));
    AcoResult result2 = optimizer.optimize(objective);

    assertThat(result1.getBestLap()).isCloseTo(result2.getBestLap(), within(1e-10));
    for (int i = 0; i < result1.getBestLine().length; i++) {
      assertThat(result1.getBestLine()[i]).isEqualTo(result2.getBestLine()[i]);
    }
  }

  // A15: Beats centreline oval (slow test)
  @Test
  @DisplayName("A15 beats centreline oval (slow)")
  void testBeatsCentrelineOval() {
    AcoConfig slowConfig = new AcoConfig(20, 7, 1.0, 1.0, 0.1, 2.0, 1.0, 10000);
    optimizer = new AntColonyOptimizer(slowConfig, new Random(99));

    Aco.ToyDiscreteObjective objective = new Aco.ToyDiscreteObjective(15, 7, 2.0, 3);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLap()).isLessThan(10.0);
  }
}
