package za.ac.uj.racinglines.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.physics.LapTimeSimulator;
import za.ac.uj.racinglines.physics.PhysicsConfig;
import za.ac.uj.racinglines.track.Track;
import za.ac.uj.racinglines.track.TrackFactory;

/** Tests for the control-point spline that turns K offsets into one offset per gate. */
class SplineObjectiveTest {

  private final LapTimeSimulator simulator =
      new LapTimeSimulator(new PhysicsConfig(1.4, 9.81, 80.0, 6.0, 12.0));
  private final Track oval = TrackFactory.ovalStandard(); // 100 gates, 12 m wide

  private LapTimeObjective withControls(int k) {
    return new LapTimeObjective(oval, simulator, 12.0, k);
  }

  @Test
  @DisplayName("SP1 dimension and bounds follow the number of control points")
  void dimensionAndBounds() {
    LapTimeObjective lap = withControls(20);
    assertThat(lap.getDimension()).isEqualTo(20);
    assertThat(lap.getLowerBounds()).hasSize(20).containsOnly(-6.0);
    assertThat(lap.getUpperBounds()).hasSize(20).containsOnly(6.0);
  }

  @Test
  @DisplayName("SP2 the spline passes through every control point")
  void passesThroughControlPoints() {
    LapTimeObjective lap = withControls(20);
    double[] c = new double[20];
    Random r = new Random(3);
    for (int i = 0; i < c.length; i++) {
      c[i] = -4 + 8 * r.nextDouble();
    }
    double[] gates = lap.toGateOffsets(c);
    assertThat(gates).hasSize(100);
    for (int k = 0; k < 20; k++) {
      assertThat(gates[k * 5]).isCloseTo(c[k], within(1e-12)); // 100 gates / 20 controls
    }
  }

  @Test
  @DisplayName("SP3 zero controls give the centreline, and the baseline matches")
  void zeroIsCentreline() {
    LapTimeObjective spline = withControls(20);
    LapTimeObjective perGate = new LapTimeObjective(oval, simulator, 12.0);
    assertThat(spline.toGateOffsets(new double[20])).containsOnly(0.0);
    assertThat(spline.baseline()).isCloseTo(perGate.baseline(), within(1e-9));
  }

  @Test
  @DisplayName("SP4 expanded offsets always stay on the track")
  void staysOnTrack() {
    LapTimeObjective lap = withControls(20);
    double[] extreme = new double[20];
    for (int i = 0; i < extreme.length; i++) {
      extreme[i] = (i % 2 == 0) ? 6.0 : -6.0; // worst case for spline overshoot
    }
    for (double g : lap.toGateOffsets(extreme)) {
      assertThat(g).isBetween(-6.0, 6.0);
    }
  }

  @Test
  @DisplayName("SP5 K equal to the gate count is the identity")
  void identityWhenKEqualsGates() {
    LapTimeObjective lap = withControls(100);
    double[] c = new double[100];
    c[7] = 2.5;
    assertThat(lap.toGateOffsets(c)).containsExactly(c);
  }

  @Test
  @DisplayName("SP6 wrong vector length is rejected")
  void wrongLength() {
    assertThatThrownBy(() -> withControls(20).evaluate(new double[100]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
