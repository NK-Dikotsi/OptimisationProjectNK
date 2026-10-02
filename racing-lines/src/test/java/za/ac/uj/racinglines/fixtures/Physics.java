package za.ac.uj.racinglines.fixtures;

import za.ac.uj.racinglines.physics.PhysicsConfig;

public class Physics {
  // Standard test configuration
  public static PhysicsConfig standardConfig() {
    return new PhysicsConfig(
        1.5,   // mu (friction)
        9.81,  // g (gravitational acceleration)
        50.0,  // vtop (top speed, m/s)
        5.0,   // aacc (acceleration, m/s²)
        8.0    // abrake (braking, m/s²)
    );
  }

  // High-friction config for corner tests
  public static PhysicsConfig highFrictionConfig() {
    return new PhysicsConfig(2.0, 9.81, 50.0, 5.0, 8.0);
  }

  // Low top-speed config to test cap
  public static PhysicsConfig lowTopSpeedConfig() {
    return new PhysicsConfig(1.5, 9.81, 10.0, 5.0, 8.0);
  }
}
