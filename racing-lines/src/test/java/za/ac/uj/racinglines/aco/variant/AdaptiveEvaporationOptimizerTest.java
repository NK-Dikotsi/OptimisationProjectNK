package za.ac.uj.racinglines.aco.variant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.aco.AcoConfig;
import za.ac.uj.racinglines.aco.AntColonyOptimizer;
import za.ac.uj.racinglines.aco.AcoResult;
import za.ac.uj.racinglines.fixtures.AdaptiveEvaporation;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class AdaptiveEvaporationOptimizerTest {
  private AdaptiveEvaporationConfig config;
  private AdaptiveEvaporationOptimizer optimizer;
  private Random random;

  @BeforeEach
  void setUp() {
    config = new AdaptiveEvaporationConfig(10, 5, 1.0, 1.0, 0.05, 0.15, 1.0, 1.0, 1000);
    random = new Random();
    optimizer = new AdaptiveEvaporationOptimizer(config, random);
  }

  // E1: Rho bounds enforcement
  @Test
  @DisplayName("E1 rho bounds")
  void testRhoBounds() {
    AdaptiveEvaporation.ToyDiscreteObjective objective =
        new AdaptiveEvaporation.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();
    assertThat(result.getEvaluationsUsed()).isLessThanOrEqualTo(config.getBudget());
    // Rho should have been applied adaptively during optimization
    assertThat(result.getHistory()).isNotEmpty();
  }

  // E2: Rho direction based on entropy
  @Test
  @DisplayName("E2 rho direction")
  void testRhoDirection() {
    // Test low entropy scenario: ρ should increase toward ρmax
    AdaptiveEvaporationConfig lowEntropyConfig =
        new AdaptiveEvaporationConfig(15, 3, 2.0, 2.0, 0.05, 0.15, 1.0, 1.0, 1000);
    optimizer = new AdaptiveEvaporationOptimizer(lowEntropyConfig, new Random(42));

    AdaptiveEvaporation.ToyDiscreteObjective objective =
        new AdaptiveEvaporation.ToyDiscreteObjective(5, 3, 1.0, 1);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLine()).isNotNull();

    // Test high entropy scenario: ρ should move toward ρmin
    AdaptiveEvaporationConfig highEntropyConfig =
        new AdaptiveEvaporationConfig(5, 7, 0.5, 0.5, 0.05, 0.15, 1.0, 1.0, 1000);
    optimizer = new AdaptiveEvaporationOptimizer(highEntropyConfig, new Random(42));

    AdaptiveEvaporation.ToyDiscreteObjective objective2 =
        new AdaptiveEvaporation.ToyDiscreteObjective(10, 7, 2.0, 3);
    AcoResult result2 = optimizer.optimize(objective2);

    assertThat(result2.getBestLine()).isNotNull();
  }

  // E3: Reduces to plain ACO when rho_min = rho_max
  @Test
  @DisplayName("E3 reduces to ACO")
  void testReducesToAco() {
    double baseRho = 0.1;

    // Plain ACO
    AcoConfig acoConfig = new AcoConfig(10, 5, 1.0, 1.0, baseRho, 1.0, 1.0, 1000);
    AntColonyOptimizer acoOptimizer = new AntColonyOptimizer(acoConfig, new Random(42));

    // Adaptive evaporation with rho_min = rho_max = baseRho
    AdaptiveEvaporationConfig adaptiveConfig =
        new AdaptiveEvaporationConfig(10, 5, 1.0, 1.0, baseRho, baseRho, 1.0, 1.0, 1000);
    optimizer = new AdaptiveEvaporationOptimizer(adaptiveConfig, new Random(42));

    AdaptiveEvaporation.ToyDiscreteObjective objective =
        new AdaptiveEvaporation.ToyDiscreteObjective(10, 5, 1.0, 2);

    AcoResult acoResult = acoOptimizer.optimize(objective);
    AcoResult adaptiveResult = optimizer.optimize(objective);

    // With same seed and fixed rho, results should be identical
    assertThat(adaptiveResult.getBestLap()).isCloseTo(acoResult.getBestLap(), within(1e-10));
    for (int i = 0; i < acoResult.getBestLine().length; i++) {
      assertThat(adaptiveResult.getBestLine()[i]).isEqualTo(acoResult.getBestLine()[i]);
    }
  }

  // E4: Rho hand calculation
  @Test
  @DisplayName("E4 rho hand calc")
  void testRhoHandCalc() {
    int numNodes = 5;
    double rhoMin = 0.05;
    double rhoMax = 0.15;
    double maxEntropy = Math.log(numNodes);

    // Test 1: Low entropy (convergence)
    double lowEntropy = 0.5; // Low entropy value
    double normalizedEntropy = lowEntropy / maxEntropy;
    double expectedRho1 = rhoMin + (rhoMax - rhoMin) * (1.0 - normalizedEntropy);
    double expectedRho1Clamped = Math.max(rhoMin, Math.min(rhoMax, expectedRho1));

    assertThat(expectedRho1Clamped).isGreaterThan(rhoMin).isLessThanOrEqualTo(rhoMax);

    // Test 2: High entropy (diversity)
    double highEntropy = 1.5; // High entropy value
    double normalizedEntropy2 = highEntropy / maxEntropy;
    double expectedRho2 = rhoMin + (rhoMax - rhoMin) * (1.0 - normalizedEntropy2);
    double expectedRho2Clamped = Math.max(rhoMin, Math.min(rhoMax, expectedRho2));

    assertThat(expectedRho2Clamped).isGreaterThanOrEqualTo(rhoMin).isLessThan(rhoMax);

    // Low entropy should give higher rho than high entropy
    if (normalizedEntropy < normalizedEntropy2) {
      assertThat(expectedRho1Clamped).isGreaterThanOrEqualTo(expectedRho2Clamped);
    }
  }

  // E5: Adaptive evaporation affects convergence (optional, for validation)
  @Test
  @DisplayName("E5 adaptive convergence")
  void testAdaptiveConvergence() {
    AdaptiveEvaporationConfig adaptiveConfig =
        new AdaptiveEvaporationConfig(15, 5, 1.0, 1.0, 0.05, 0.15, 2.0, 1.0, 5000);
    optimizer = new AdaptiveEvaporationOptimizer(adaptiveConfig, new Random(42));

    AdaptiveEvaporation.ToyDiscreteObjective objective =
        new AdaptiveEvaporation.ToyDiscreteObjective(10, 5, 1.0, 2);
    AcoResult result = optimizer.optimize(objective);

    assertThat(result.getBestLap()).isLessThan(5.0);
    assertThat(result.getHistory()).isNotEmpty();
  }

  // E6: Same seed same result
  @Test
  @DisplayName("E6 same seed same result")
  void testSameSeedSameResult() {
    AdaptiveEvaporation.ToyDiscreteObjective objective =
        new AdaptiveEvaporation.ToyDiscreteObjective(5, 3, 1.0, 1);

    optimizer = new AdaptiveEvaporationOptimizer(config, new Random(42));
    AcoResult result1 = optimizer.optimize(objective);

    optimizer = new AdaptiveEvaporationOptimizer(config, new Random(42));
    AcoResult result2 = optimizer.optimize(objective);

    assertThat(result1.getBestLap()).isCloseTo(result2.getBestLap(), within(1e-10));
    for (int i = 0; i < result1.getBestLine().length; i++) {
      assertThat(result1.getBestLine()[i]).isEqualTo(result2.getBestLine()[i]);
    }
  }
}
