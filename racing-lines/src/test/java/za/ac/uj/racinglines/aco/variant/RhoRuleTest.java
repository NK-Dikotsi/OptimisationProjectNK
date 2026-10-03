package za.ac.uj.racinglines.aco.variant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the adaptive evaporation ρ rule calculation. */
class RhoRuleTest {

  @Test
  @DisplayName("RR1 nextRho hand-calculated for RAISE_WHEN_CONVERGED")
  void raiseWhenConvergedValues() {
    // convergence c = 1 - entropy (0 = all nodes equally likely, 1 = single node per gate)
    // RAISE: rho = rhoMin + (rhoMax - rhoMin) * c
    double rho0 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 1.0,
        AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
    assertThat(rho0).isCloseTo(0.05, within(1e-9)); // no convergence: c=0, rho=rhoMin

    double rho05 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 0.5,
        AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
    assertThat(rho05).isCloseTo(0.175, within(1e-9)); // half converged: c=0.5, rho=0.05+(0.25)*0.5

    double rho1 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 0.0,
        AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
    assertThat(rho1).isCloseTo(0.3, within(1e-9)); // full convergence: c=1, rho=rhoMax
  }

  @Test
  @DisplayName("RR2 nextRho hand-calculated for LOWER_WHEN_CONVERGED")
  void lowerWhenConvergedValues() {
    // LOWER: rho = rhoMax - (rhoMax - rhoMin) * c
    double rho0 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 1.0,
        AdaptiveEvaporationConfig.RhoRule.LOWER_WHEN_CONVERGED);
    assertThat(rho0).isCloseTo(0.3, within(1e-9)); // no convergence: c=0, rho=rhoMax

    double rho05 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 0.5,
        AdaptiveEvaporationConfig.RhoRule.LOWER_WHEN_CONVERGED);
    assertThat(rho05).isCloseTo(0.175, within(1e-9)); // half converged: c=0.5, rho=0.3-(0.25)*0.5

    double rho1 = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, 0.0,
        AdaptiveEvaporationConfig.RhoRule.LOWER_WHEN_CONVERGED);
    assertThat(rho1).isCloseTo(0.05, within(1e-9)); // full convergence: c=1, rho=rhoMin
  }

  @Test
  @DisplayName("RR3 ρ always stays within the bounds")
  void rhoBounded() {
    for (double entropy = 0.0; entropy <= 1.0; entropy += 0.1) {
      double rhoRaise = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, entropy,
          AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
      double rhoLower = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, entropy,
          AdaptiveEvaporationConfig.RhoRule.LOWER_WHEN_CONVERGED);

      assertThat(rhoRaise).isBetween(0.05, 0.3);
      assertThat(rhoLower).isBetween(0.05, 0.3);
    }
  }

  @Test
  @DisplayName("RR4 the two rules are exact mirrors: RAISE + LOWER = rhoMin + rhoMax")
  void rulesMirror() {
    for (double entropy = 0.0; entropy <= 1.0; entropy += 0.1) {
      double rhoRaise = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, entropy,
          AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
      double rhoLower = AdaptiveEvaporationOptimizer.nextRho(0.05, 0.3, entropy,
          AdaptiveEvaporationConfig.RhoRule.LOWER_WHEN_CONVERGED);

      assertThat(rhoRaise + rhoLower).isCloseTo(0.05 + 0.3, within(1e-9));
    }
  }

  @Test
  @DisplayName("RR5 the default constructor keeps the original rule")
  void defaultRuleIsRaise() {
    AdaptiveEvaporationConfig config = new AdaptiveEvaporationConfig(
        20, 11, 1.0, 2.0, 0.05, 0.3, 100.0, 10.0, 0.1, 10.0, 5000);
    assertThat(config.getRule()).isEqualTo(AdaptiveEvaporationConfig.RhoRule.RAISE_WHEN_CONVERGED);
  }
}
