# Airplane Simulator

Software Testing, assignment 2: **Mocks Aren't Stubs**.

Starter: [UT-SWT/F05-CA2](https://github.com/UT-SWT/F05-CA2), revision
`ee770866a602de06785bcd8a06585398d50a52e2`.

## Run

Requires JDK 17 or newer. The wrapper downloads Maven 3.9.9 on first use;
the first build also needs access to Maven Central for dependencies.

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

Linux/macOS:

```sh
./mvnw clean verify
./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

`test` runs the JUnit suite. `verify` also generates JaCoCo reports and checks
100% line and branch coverage across all production classes. The separate
`mutation` profile checks whether the suite detects changes injected by PIT.

Generated reports:

- `target/surefire-reports/`: test results.
- `target/site/jacoco/index.html`: coverage report.
- `target/pit-reports/index.html`: mutation report after running PIT.

See [the test map and change report](docs/TESTING.md) for the assignment checklist,
and [the Persian study guide](docs/STUDY_GUIDE.fa.md) for explanations and oral
delivery practice. GitHub Actions is configured to verify Java 17 and 21 and run PIT.

## Flight behavior

1. A new aircraft starts in `GROUNDED` state.
2. `prepareForTakeoff()` checks that the engine is operational, enough fuel is
   available, and a route is available. On success, the state becomes `READY`.
3. `takeOff()` starts the engine, retracts the landing gear, and changes the
   state to `IN_FLIGHT`.
4. `cruise(amount)` consumes fuel. If the engine is no longer operational, the
   aircraft makes an emergency landing. A fuel-consumption failure also causes
   an emergency landing and is reported as a `FlightException`.
5. `land()` and `emergencyLand()` deploy the landing gear, stop the engine,
   and change the state to `LANDED`.

An invalid lifecycle operation (for example, calling `takeOff()` before
preparation) throws `FlightException`.

- `FlightException` is an unchecked exception.
- `IllegalArgumentException` is used for invalid numeric input.
- Fuel values must be finite. Initial fuel and the takeoff minimum may be zero;
  cruise and consumption amounts must be strictly positive. Invalid input leaves
  the state and fuel unchanged.
- `Engine.fail()` can simulate an engine failure during a flight.
- `NavigationSystem.setRouteAvailable(...)` can change route availability.
- Mockito and JUnit 5 dependencies are already configured with test scope in `pom.xml`.
