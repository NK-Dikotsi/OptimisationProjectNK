package za.ac.uj.racinglines.physics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PhysicsConfig {
  private final double mu;           // friction coefficient
  private final double g;            // gravitational acceleration (m/s²)
  private final double vtop;         // top speed (m/s)
  private final double aacc;         // acceleration (m/s²)
  private final double abrake;       // braking deceleration (m/s²)
}
