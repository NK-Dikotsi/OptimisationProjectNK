package za.ac.uj.racinglines.physics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.uj.racinglines.fixtures.Physics;

import static org.assertj.core.api.Assertions.*;

class LapTimeSimulatorTest {
  private LapTimeSimulator simulator;
  private PhysicsConfig config;

  @BeforeEach
  void setUp() {
    config = Physics.standardConfig();
    simulator = new LapTimeSimulator(config);
  }

  // P1: Curvature validation for circle
  @Test
  @DisplayName("P1 circle curvature")
  void testCurvatureCircle() {
    int n = 360;
    double radius = 50.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    double[] kappa = simulator.curvature(circle);
    double expectedKappa = 1.0 / radius;

    for (int i = 0; i < n; i++) {
      assertThat(kappa[i]).isCloseTo(expectedKappa, within(expectedKappa * 1e-3));
    }
  }

  // P2: Curvature for straight line
  @Test
  @DisplayName("P2 straight line curvature")
  void testCurvatureStraight() {
    double[][] line = new double[100][2];
    for (int i = 0; i < 100; i++) {
      line[i][0] = i;
      line[i][1] = 0;
    }

    double[] kappa = simulator.curvature(line);

    for (int i = 0; i < 100; i++) {
      assertThat(kappa[i]).isCloseTo(0.0, within(1e-10));
      assertThat(kappa[i]).isFinite();
    }
  }

  // P3: Curvature direction independent
  @Test
  @DisplayName("P3 curvature direction independent")
  void testCurvatureDirectionIndependent() {
    int n = 180;
    double radius = 30.0;

    // Clockwise
    double[][] clockwise = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      clockwise[i][0] = radius * Math.cos(theta);
      clockwise[i][1] = radius * Math.sin(theta);
    }
    double[] kappaClockwise = simulator.curvature(clockwise);

    // Counter-clockwise
    double[][] counterClockwise = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * (n - i) / n;
      counterClockwise[i][0] = radius * Math.cos(theta);
      counterClockwise[i][1] = radius * Math.sin(theta);
    }
    double[] kappaCounterClockwise = simulator.curvature(counterClockwise);

    for (int i = 0; i < n; i++) {
      assertThat(Math.abs(kappaClockwise[i])).isCloseTo(Math.abs(kappaCounterClockwise[i]), within(1e-10));
    }
  }

  // P4: Corner speed on circle
  @Test
  @DisplayName("P4 corner speed on circle")
  void testCornerSpeedCircle() {
    int n = 360;
    double radius = 50.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    double[] kappa = simulator.curvature(circle);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double expectedSpeed = Math.sqrt(config.getMu() * config.getG() * radius);

    for (int i = 0; i < n; i++) {
      assertThat(vmax[i]).isCloseTo(expectedSpeed, within(expectedSpeed * 1e-2));
    }
  }

  // P5: Top speed cap on long straight
  @Test
  @DisplayName("P5 top speed cap")
  void testTopSpeedCap() {
    int n = 500;
    double[][] straight = new double[n][2];
    for (int i = 0; i < n; i++) {
      straight[i][0] = i;
      straight[i][1] = 0;
    }

    double[] kappa = simulator.curvature(straight);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, simulator.segmentLengths(straight));

    for (int i = 0; i < n; i++) {
      assertThat(v[i]).isLessThanOrEqualTo(config.getVtop() + 1e-6);
    }
  }

  // P6: Circle lap time analytical validation (KEY TEST)
  @Test
  @DisplayName("P6 circle lap time analytic")
  void testCircleLapTimeAnalytic() {
    int n = 360;
    double radius = 50.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    double lapTime = simulator.lapTime(circle);
    double expectedLapTime = 2.0 * Math.PI * Math.sqrt(radius / (config.getMu() * config.getG()));

    assertThat(lapTime).isCloseTo(expectedLapTime, within(expectedLapTime * 1e-3));
  }

  // P7: Inner line faster than outer on circle
  @Test
  @DisplayName("P7 inner line faster on circle")
  void testInnerLineFasterOnCircle() {
    int n = 360;
    double outerRadius = 50.0;
    double innerRadius = 45.0;

    // Outer line
    double[][] outerCircle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      outerCircle[i][0] = outerRadius * Math.cos(theta);
      outerCircle[i][1] = outerRadius * Math.sin(theta);
    }
    double outerLapTime = simulator.lapTime(outerCircle);

    // Inner line
    double[][] innerCircle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      innerCircle[i][0] = innerRadius * Math.cos(theta);
      innerCircle[i][1] = innerRadius * Math.sin(theta);
    }
    double innerLapTime = simulator.lapTime(innerCircle);

    assertThat(innerLapTime).isLessThan(outerLapTime);
  }

  // P8: Speed never exceeds vmax (property)
  @Test
  @DisplayName("P8 speed below limit")
  void testSpeedBelowLimit() {
    int n = 200;
    double[][] ovalLine = new double[n][2];
    for (int i = 0; i < n; i++) {
      double t = 2.0 * Math.PI * i / n;
      ovalLine[i][0] = 50.0 * Math.cos(t);
      ovalLine[i][1] = 25.0 * Math.sin(t);
    }

    double[] kappa = simulator.curvature(ovalLine);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, simulator.segmentLengths(ovalLine));

    for (int i = 0; i < n; i++) {
      assertThat(v[i]).isLessThanOrEqualTo(vmax[i] + 1e-6);
    }
  }

  // P9: Acceleration limit (property)
  @Test
  @DisplayName("P9 accel limit")
  void testAccelLimit() {
    int n = 200;
    double[][] line = new double[n][2];
    for (int i = 0; i < n; i++) {
      line[i][0] = i;
      line[i][1] = 0;
    }

    double[] segLengths = simulator.segmentLengths(line);
    double[] kappa = simulator.curvature(line);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, segLengths);

    for (int i = 0; i < n - 1; i++) {
      double dvSquared = v[i + 1]*v[i + 1] - v[i]*v[i];
      assertThat(dvSquared).isLessThanOrEqualTo(2.0 * config.getAacc() * segLengths[i] + 1e-6);
    }
  }

  // P10: Brake limit (property)
  @Test
  @DisplayName("P10 brake limit")
  void testBrakeLimit() {
    int n = 200;
    double[][] line = new double[n][2];
    for (int i = 0; i < n; i++) {
      line[i][0] = i;
      line[i][1] = 0;
    }

    double[] segLengths = simulator.segmentLengths(line);
    double[] kappa = simulator.curvature(line);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, segLengths);

    for (int i = 0; i < n - 1; i++) {
      double dvSquared = v[i]*v[i] - v[i + 1]*v[i + 1];
      assertThat(dvSquared).isLessThanOrEqualTo(2.0 * config.getAbrake() * segLengths[i] + 1e-6);
    }
  }

  // P11: Loop continuity (start speed = end speed)
  @Test
  @DisplayName("P11 loop continuity")
  void testLoopContinuity() {
    int n = 360;
    double radius = 40.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    double[] kappa = simulator.curvature(circle);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, simulator.segmentLengths(circle));

    assertThat(v[0]).isCloseTo(v[n - 1], within(1e-6));
  }

  // P12: Braking point hand calculation
  @Test
  @DisplayName("P12 braking point hand calc")
  void testBrakingPointHandCalc() {
    int totalSegments = 500;
    double straightLength = 400.0;
    double cornerRadius = 30.0;
    int cornerSegments = totalSegments - (int)(straightLength / (1.0));

    // Create: long straight + sharp corner
    double[][] line = new double[totalSegments][2];
    for (int i = 0; i < totalSegments - cornerSegments; i++) {
      line[i][0] = i;
      line[i][1] = 0;
    }

    // Sharp corner (quarter circle)
    int cornerStart = totalSegments - cornerSegments;
    for (int i = 0; i < cornerSegments; i++) {
      double t = Math.PI * i / (2.0 * cornerSegments);
      line[cornerStart + i][0] = straightLength + cornerRadius * Math.sin(t);
      line[cornerStart + i][1] = cornerRadius * (1.0 - Math.cos(t));
    }

    double[] kappa = simulator.curvature(line);
    double[] vmax = simulator.maxSpeedByFriction(kappa);
    double[] v = simulator.speedProfile(vmax, simulator.segmentLengths(line));

    // Speed should transition from vtop to corner speed
    double cornerSpeed = Math.sqrt(config.getMu() * config.getG() * cornerRadius);
    assertThat(v[0]).isCloseTo(config.getVtop(), within(1.0));
    assertThat(v[cornerStart]).isCloseTo(cornerSpeed, within(cornerSpeed * 0.15));
  }

  // P13: More grip never increases lap time (property)
  @Test
  @DisplayName("P13 more grip not slower")
  void testMoreGripNotSlower() {
    int n = 360;
    double radius = 50.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    PhysicsConfig lowMu = new PhysicsConfig(1.0, config.getG(), config.getVtop(), config.getAacc(), config.getAbrake());
    PhysicsConfig highMu = new PhysicsConfig(1.8, config.getG(), config.getVtop(), config.getAacc(), config.getAbrake());

    LapTimeSimulator simLowMu = new LapTimeSimulator(lowMu);
    LapTimeSimulator simHighMu = new LapTimeSimulator(highMu);

    double lapTimeLowMu = simLowMu.lapTime(circle);
    double lapTimeHighMu = simHighMu.lapTime(circle);

    assertThat(lapTimeHighMu).isLessThanOrEqualTo(lapTimeLowMu + 1e-6);
  }

  // P14: Lap time finite and positive (property)
  @Test
  @DisplayName("P14 lap time finite positive")
  void testLapTimeFinitePositive() {
    int n = 300;
    double[][] oval = new double[n][2];
    for (int i = 0; i < n; i++) {
      double t = 2.0 * Math.PI * i / n;
      oval[i][0] = 60.0 * Math.cos(t);
      oval[i][1] = 40.0 * Math.sin(t);
    }

    double lapTime = simulator.lapTime(oval);

    assertThat(lapTime).isFinite();
    assertThat(lapTime).isGreaterThan(0.0);
  }

  // P15: Resolution convergence
  @Test
  @DisplayName("P15 resolution convergence")
  void testResolutionConvergence() {
    double radius = 50.0;

    // N = 500
    int n500 = 500;
    double[][] circle500 = new double[n500][2];
    for (int i = 0; i < n500; i++) {
      double theta = 2.0 * Math.PI * i / n500;
      circle500[i][0] = radius * Math.cos(theta);
      circle500[i][1] = radius * Math.sin(theta);
    }
    double lapTime500 = simulator.lapTime(circle500);

    // N = 1000
    int n1000 = 1000;
    double[][] circle1000 = new double[n1000][2];
    for (int i = 0; i < n1000; i++) {
      double theta = 2.0 * Math.PI * i / n1000;
      circle1000[i][0] = radius * Math.cos(theta);
      circle1000[i][1] = radius * Math.sin(theta);
    }
    double lapTime1000 = simulator.lapTime(circle1000);

    double relError = Math.abs(lapTime500 - lapTime1000) / lapTime1000;
    assertThat(relError).isLessThan(0.01);
  }

  // P16: Evaluation counter
  @Test
  @DisplayName("P16 eval counter")
  void testEvalCounter() {
    int n = 100;
    double[][] line = new double[n][2];
    for (int i = 0; i < n; i++) {
      line[i][0] = i;
      line[i][1] = 0;
    }

    long countBefore = simulator.getEvaluationCount();
    simulator.lapTime(line);
    long countAfter = simulator.getEvaluationCount();

    assertThat(countAfter - countBefore).isEqualTo(1);

    simulator.lapTime(line);
    assertThat(simulator.getEvaluationCount() - countAfter).isEqualTo(1);
  }

  // P17: Centreline golden baseline (regression)
  @Test
  @DisplayName("P17 centreline golden")
  void testCentrelineGolden() {
    // Circle track
    int n = 360;
    double radius = 50.0;
    double[][] circle = new double[n][2];
    for (int i = 0; i < n; i++) {
      double theta = 2.0 * Math.PI * i / n;
      circle[i][0] = radius * Math.cos(theta);
      circle[i][1] = radius * Math.sin(theta);
    }

    double baselineLapTime = simulator.baselineLapTime(circle, "circle");
    double expectedCircleLapTime = 2.0 * Math.PI * Math.sqrt(radius / (config.getMu() * config.getG()));

    assertThat(baselineLapTime).isCloseTo(expectedCircleLapTime, within(expectedCircleLapTime * 1e-3));
  }
}
