# Real Circuit Demonstration - Spielberg (Red Bull Ring)

This is a separate Maven project that demonstrates the racing-lines optimization pipeline on a real circuit from the TUM racetrack database.

## Project Structure

```
Spielberg/
├── pom.xml                                 # Maven configuration
├── src/main/java/.../realtrack/
│   ├── TumTrack.java                      # TUM track data loader
│   └── RealTrackExperiment.java           # Main optimization runner
├── src/test/java/.../realtrack/
│   └── TumTrackTest.java                  # 6 loader tests (RT1-RT6)
├── data/
│   ├── Spielberg.csv                      # Centreline and track widths
│   ├── Spielberg_raceline.csv             # TUM reference minimum-curvature line
│   └── LICENSE                            # LGPL v3 for TUM data
└── runs/                                   # Output folder (gitignored)
```

## Building

The Spielberg project depends on the main racing-lines project as a library. Build the main project first:

```bash
cd ..
mvn -q install -DskipTests
cd Spielberg
```

## Testing

Run the six track loader tests:

```bash
mvn test
```

Tests verify:
- **RT1**: Load a real track from CSV
- **RT2**: Track length computation
- **RT3**: Track width extraction
- **RT4**: Min and median half-widths
- **RT5**: Conversion to internal Track model
- **RT6**: Polyline loading (reference race line)

## Running the Optimization

The main class reuses the entire racing-lines pipeline unchanged (same physics, same algorithms, same engine). It scales control points to the track length so they stay about 25 m apart, as on the main tracks.

```bash
mvn -q compile exec:java "-Dexec.args=data/Spielberg.csv Spielberg 100000 3 data/Spielberg_raceline.csv"
```

Arguments:
- `data/Spielberg.csv` - Track centreline and widths
- `Spielberg` - Track name
- `100000` - Budget per run (evaluations)
- `3` - Number of seeds (42, 43, ...)
- `data/Spielberg_raceline.csv` - Optional reference race line

Output:
- `runs/realtrack/Spielberg_<timestamp>/info.txt` - Track and experiment info
- `runs/realtrack/Spielberg_<timestamp>/results.csv` - Results summary (PSO and ACO, 3 seeds each)
- `runs/realtrack/Spielberg_<timestamp>/<algorithm>_<seed>/` - Convergence and best line for each run

## Data Source

**TUM Racetrack Database** — https://github.com/TUMFTM/racetrack-database

Spielberg (Red Bull Ring) track geometry and TUM's minimum-curvature reference race line, computed with:

> Heilmeier, A. et al. (2020), *Minimum curvature trajectory planning and control for an autonomous race car*, Vehicle System Dynamics, 58(10), pp. 1497-1527.

License: GNU LGPL v3 (see LICENSE in this folder).

## Notes

- This project is **not** part of the main racing-lines build; `mvn test` in the parent directory ignores it.
- The Spielberg project must be built and tested independently from within its own directory.
- All optimization parameters (physics, algorithms, control-point spacing) are inherited from the main pipeline unchanged.
