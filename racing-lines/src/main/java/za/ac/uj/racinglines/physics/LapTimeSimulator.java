package za.ac.uj.racinglines.physics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Getter
@RequiredArgsConstructor
public class LapTimeSimulator {
  private static final Logger logger = LoggerFactory.getLogger(LapTimeSimulator.class);

  private final PhysicsConfig config;
  private long evaluationCount = 0;

  /**
   * Compute curvature at each point on a line using discrete geometry.
   * κ[i] = 1/R where R is the radius of curvature.
   * Returns signed curvature (positive = left turn, negative = right turn).
   */
  public double[] curvature(double[][] line) {
    int n = line.length;
    double[] kappa = new double[n];

    for (int i = 0; i < n; i++) {
      int prev = (i - 1 + n) % n;
      int next = (i + 1) % n;

      double x0 = line[prev][0], y0 = line[prev][1];
      double x1 = line[i][0],    y1 = line[i][1];
      double x2 = line[next][0], y2 = line[next][1];

      double dx1 = x1 - x0, dy1 = y1 - y0;
      double dx2 = x2 - x1, dy2 = y2 - y1;

      double ds1 = Math.sqrt(dx1*dx1 + dy1*dy1);
      double ds2 = Math.sqrt(dx2*dx2 + dy2*dy2);

      if (ds1 < 1e-10 || ds2 < 1e-10) {
        kappa[i] = 0.0;
      } else {
        double cross = dx1*dy2 - dy1*dx2;
        double denom = ds1 * ds2 * (ds1 + ds2);
        kappa[i] = 2.0 * cross / (denom + 1e-12);
      }
    }

    return kappa;
  }

  /**
   * Compute segment lengths between consecutive points.
   */
  public double[] segmentLengths(double[][] line) {
    int n = line.length;
    double[] lengths = new double[n];

    for (int i = 0; i < n; i++) {
      int next = (i + 1) % n;
      double dx = line[next][0] - line[i][0];
      double dy = line[next][1] - line[i][1];
      lengths[i] = Math.sqrt(dx*dx + dy*dy);
    }

    return lengths;
  }

  /**
   * Compute max speed at each point due to friction limit.
   * vmax[i] = min(sqrt(mu*g/|κ[i]|), vtop)
   */
  public double[] maxSpeedByFriction(double[] kappa) {
    int n = kappa.length;
    double[] vmax = new double[n];

    for (int i = 0; i < n; i++) {
      double absKappa = Math.abs(kappa[i]);
      if (absKappa < 1e-10) {
        vmax[i] = config.getVtop();
      } else {
        double v = Math.sqrt(config.getMu() * config.getG() / absKappa);
        vmax[i] = Math.min(v, config.getVtop());
      }
    }

    return vmax;
  }

  /**
   * Compute speed profile through the line using forward and backward passes.
   * Iterates until convergence.
   */
  public double[] speedProfile(double[] vmax, double[] segmentLengths) {
    int n = vmax.length;
    double[] v = new double[n];

    // Start from the slowest corner to ensure closure
    int slowestIdx = 0;
    for (int i = 1; i < n; i++) {
      if (vmax[i] < vmax[slowestIdx]) {
        slowestIdx = i;
      }
    }
    v[slowestIdx] = vmax[slowestIdx];

    // Iterate forward and backward passes until convergence
    boolean converged = false;
    int maxIter = 100;
    int iter = 0;

    while (!converged && iter < maxIter) {
      iter++;
      converged = true;

      // Forward pass
      for (int i = 0; i < n; i++) {
        int next = (i + 1) % n;
        double vNextMax = Math.sqrt(v[i]*v[i] + 2.0 * config.getAacc() * segmentLengths[i]);
        double vNew = Math.min(vmax[next], vNextMax);
        if (Math.abs(vNew - v[next]) > 1e-8) {
          converged = false;
        }
        v[next] = vNew;
      }

      // Backward pass
      for (int i = n - 1; i >= 0; i--) {
        int next = (i + 1) % n;
        double vPrevMax = Math.sqrt(v[next]*v[next] + 2.0 * config.getAbrake() * segmentLengths[i]);
        double vNew = Math.min(vmax[i], vPrevMax);
        if (Math.abs(vNew - v[i]) > 1e-8) {
          converged = false;
        }
        v[i] = vNew;
      }
    }

    return v;
  }

  /**
   * Calculate lap time for a given racing line.
   * T = sum(2*s[i] / (v[i] + v[i+1]))
   */
  public double lapTime(double[][] line) {
    try {
      double[] kappa = curvature(line);
      double[] segLengths = segmentLengths(line);
      double[] vmax = maxSpeedByFriction(kappa);
      double[] v = speedProfile(vmax, segLengths);

      evaluationCount++;

      double lapTimeValue = 0.0;
      int n = line.length;
      double minSpeed = v[0];
      double maxSpeed = v[0];

      for (int i = 0; i < n; i++) {
        int next = (i + 1) % n;
        double vAvg = (v[i] + v[next]) / 2.0;
        if (vAvg < 1e-10) vAvg = 1e-10;

        lapTimeValue += 2.0 * segLengths[i] / (v[i] + v[next]);
        minSpeed = Math.min(minSpeed, v[i]);
        maxSpeed = Math.max(maxSpeed, v[i]);
      }

      logger.debug("Lap time evaluation", kv("lap_time_s", lapTimeValue),
          kv("min_speed_ms", minSpeed), kv("max_speed_ms", maxSpeed),
          kv("eval_number", evaluationCount));

      // Check for curvature spikes
      for (int i = 0; i < n; i++) {
        if (Math.abs(kappa[i]) > 1e-3) {
          double radius = 1.0 / Math.abs(kappa[i]);
          if (radius < 2.0) {
            logger.warn("Curvature spike", kv("index", i), kv("radius_m", radius));
          }
        }
      }

      return lapTimeValue;
    } catch (Exception e) {
      logger.error("NaN lap time", kv("error", e.getMessage()));
      return Double.NaN;
    }
  }

  /**
   * Compute baseline lap time for centreline (no offsets).
   */
  public double baselineLapTime(double[][] centreline, String trackName) {
    double lapTimeValue = lapTime(centreline);

    double[] kappa = curvature(centreline);
    double[] segLengths = segmentLengths(centreline);
    double[] vmax = maxSpeedByFriction(kappa);
    double[] v = speedProfile(vmax, segLengths);

    double minSpeed = v[0];
    double maxSpeed = v[0];
    for (double speed : v) {
      minSpeed = Math.min(minSpeed, speed);
      maxSpeed = Math.max(maxSpeed, speed);
    }

    logger.info("Baseline computed", kv("track", trackName),
        kv("centreline_lap_s", lapTimeValue), kv("min_speed_ms", minSpeed),
        kv("max_speed_ms", maxSpeed));

    return lapTimeValue;
  }
}
