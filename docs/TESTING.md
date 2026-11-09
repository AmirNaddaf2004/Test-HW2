# Test map and change report

## Assignment checklist

The assignment asks for at least six tests of the aircraft simulator and at least
one use of each listed Mockito operation. It allows AI assistance and puts most
of the grade on explaining the tests during delivery. It does not specify a
separate bonus task. Coverage, mutation testing, component tests, and CI are
additional checks rather than promised bonus marks.

The starter's production sources and POM matched the original project at
`ee770866a602de06785bcd8a06585398d50a52e2`. The team's initial revision was
`016a2b0fdcff33aeacb5a244674b0c9a84027002`, with 16 passing test invocations.

All required Mockito operations appear in
[`AircraftTest`](../src/test/java/airplane/AircraftTest.java):

| Operation | Example location | What it establishes |
| --- | --- | --- |
| `mock()` | `setUp` | Inject replaceable components into the real aircraft. |
| `reset()` | `reset_RemovesStubsAndInteractionsBeforeReconfiguringTheSameMock` | Remove both stubs and recorded calls, then reconfigure the same object. |
| `when()` | `configureAllComponentsReadyForTakeoff` | Select a query whose response should be controlled. |
| `thenReturn()` | `cruise_WhenEngineFailsAfterOneSuccessfulCruise_LandsWithoutConsumingMoreFuel` | Return `true`, then `false`, on successive engine checks. |
| `verify()` | `takeOff_WhenAircraftIsReady_StartsEngineRetractsGearAndEntersInFlight` | Check that the aircraft called its collaborators. |
| `times()` | `cruise_WhenEngineIsOperationalAndFuelSufficient_ConsumesFuelAndStaysInFlight` | Check exactly two consumption calls. |
| `any()` | The same cruise test | Match any non-null `Double` value when counting consumption calls. Exact argument checks and a captor also check the actual amounts. |
| `thenThrow()` | `cruise_WhenEngineCheckThrowsRuntimeExceptionInCruise_PropagatesException` | Simulate failure of a value-returning dependency query. |
| `doNothing()` | The successful takeoff test | Explicitly configure void methods. This is already the default for ordinary mocks, but is shown to meet the exercise. |

`doThrow()` covers failures of void methods. `InOrder` checks sequencing,
`ArgumentCaptor` checks recorded arguments, and negative verifications catch
unwanted side effects. The ordinary test setup uses fresh mocks for each test.
`clearInvocations()` in flight setup removes setup calls while keeping stubs;
the reset example deliberately demonstrates the stronger operation.

## Test structure

| Class | Invocations | Scope |
| --- | ---: | --- |
| `AircraftTest` | 25 | Mocked readiness checks, takeoff and gear failures, fuel use, engine failure, landing order, reset, invalid cruise amounts. |
| `AircraftStateTest` | 32 | Initial state, four null dependencies, all 19 invalid operation/state pairs, readiness exceptions, failed re-preparation, state validation before amount validation. |
| `AircraftIntegrationTest` | 6 | Actual engine, gear, fuel, and navigation across complete flights, emergency paths, route recovery, and invalid fuel input. |
| `EngineTest` | 4 | Startup, shutdown, non-operational engine, failure and rejected restart. |
| `FuelSystemTest` | 26 | Exact thresholds, repeated/exact consumption, smallest positive amount, invalid numeric values, and insufficient fuel without mutation. |
| `LandingGearTest` | 1 | Initially deployed, retract, deploy again. |
| `NavigationSystemTest` | 6 | Destination, both route availability values, null/empty/blank destinations. |
| **Total** | **100** | 47 test methods; parameterized cases count separately. No external services are used. |

## State model

| Operation | Allowed starting states | Successful ending state |
| --- | --- | --- |
| `prepareForTakeoff` | `GROUNDED`, `LANDED` | `READY` |
| `takeOff` | `READY` | `IN_FLIGHT` |
| `cruise` | `IN_FLIGHT` | `IN_FLIGHT`, or `LANDED` on component failure |
| `land`, `emergencyLand` | `IN_FLIGHT` | `LANDED` |

Every other operation/state pair throws `FlightException`, preserves the state,
and makes no collaborator calls. Readiness checks stop at the first failed check.
Takeoff component exceptions produce `ABORTED` and retain the original cause.

## Changes from the initial revision

- `.gitignore`: ignore Maven output and local inspection files under `tmp`.
- `.gitattributes`: stable source/document line endings and platform-appropriate
  wrapper endings. The Unix wrapper is executable in Git.
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`: official Apache
  script-only wrapper, version 3.3.4, with Maven pinned to 3.9.9.
- `AircraftTest`: retain the initial scenarios, strengthen state/cause/argument
  assertions and forbidden interactions, check call order, add gear failure,
  explicit emergency landing, sequential engine failure, reset demonstration,
  and input boundaries. Avoid unintentionally relying on `false` after reset.
- `AircraftStateTest`: add constructor and exhaustive invalid-state checks.
- The four component test classes and `AircraftIntegrationTest`: test the real
  collaborators as well as the isolated aircraft.
- `Aircraft.cruise` and `FuelSystem`: reject non-finite values with
  `IllegalArgumentException` before mutation or dependency calls. Existing
  behavior for valid input remains the same. Exception messages now describe
  the finite-value requirement.
- `pom.xml`: coverage agent/report/check, optional PIT profile with its JUnit 5
  adapter, failure when no tests are found, and an explicit Mockito Java agent
  to avoid dynamic self-attachment on newer JVMs. JaCoCo's agent is preserved
  through late `argLine` evaluation. Existing dependency versions are retained.
- `.github/workflows/tests.yml`: Java 17/21 verification and a separate mutation
  job, with reports retained as artifacts for 14 days.
- `README.md` and these guides: execution instructions, requirement traceability,
  behavior notes, and preparation for delivery.

## Fuel regression

Originally `NaN <= 0` and `NaN < 0` both evaluated to false, so `NaN` passed the
guards. Subtracting it made the remaining fuel `NaN`. Positive infinity was
also accepted at construction; infinite cruise input could reach collaborators
or be misclassified as insufficient fuel. `Double.isFinite` now rejects these
inputs explicitly.

Before the fix, the new regression suite produced **8 failures**. After the fix,
all cases passed. Negative infinity, zero, negative zero, negative amounts,
exact thresholds, exact depletion, and the smallest positive finite value are
also exercised. These checks do not introduce a minimum positive amount.

## Verification

Measured locally on Windows with JDK 21.0.6, Maven 3.9.9, JUnit 5.11.4,
Mockito 5.14.2, and JaCoCo 0.8.14:

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

The clean verification run passed 100 invocations with zero failures, errors,
or skipped tests. JaCoCo measured **112/112 lines** and **38/38 branches** across
all seven production classes, with no excluded classes. `verify` enforces both
ratios at 100%. Coverage reports are generated files and remain outside Git.

PIT 1.19.4 with the JUnit 5 adapter 1.2.3 generated **51 mutants**, and all **51
were killed**. There were zero survivors, uncovered mutants, timeouts, or run
errors. Its line coverage was **102/102 lines** for mutated classes only; unlike
JaCoCo's whole-project count, this omits classes with no applicable mutations.
PIT uses its default mutators across all `airplane.*` production classes and all
`airplane.*Test` classes. Its configuration requires both mutation line coverage
and mutation score at 100%. See the generated report for per-mutant results.
The score measures this configured operator set, not every possible bug.

The GitHub Actions configuration has been added locally; remote jobs have not
been run here. Java 17 is the compiler release target; the local execution above
used Java 21.

## Behavior inherited from the starter

These points matter when interpreting tests; this is an educational simulator:

- A query throwing during readiness or during the engine health check propagates
  its exception. An engine query returning `false` is a different case: during
  cruise it triggers emergency landing without throwing.
- A fuel-consumption exception triggers landing, then becomes the cause of a
  `FlightException`, assuming the landing itself succeeds.
- Gear or engine shutdown failures during landing are not recovered by the
  starter. A failed gear retraction marks the flight `ABORTED` without a rollback
  that stops the already-started engine. These policies were not redefined.
- Route availability is checked during preparation, not continuously during
  flight. `ABORTED` has no recovery operation. There is no refueling API.
- Full coverage does not prove all possible sequences or failure combinations.
  Interaction assertions, boundary cases, integration tests, and mutation
  testing provide different evidence about correctness.

## Delivery

Keep the repository private through the end of **1405/08/23**, then make it
public as required by the assignment. Submit the repository URL and the final
commit hash in Elearn before the deadline; one team member's submission is enough.
Use `git rev-parse HEAD` for the hash after all intended changes are committed.
The study guide explains the tests, but both team members should practice tracing
them and answering the questions in their own words.

## References

- [Starter project](https://github.com/UT-SWT/F05-CA2)
- [Martin Fowler: Mocks Aren't Stubs](https://martinfowler.com/articles/mocksArentStubs.html)
- [Mockito 5.14.2 API](https://javadoc.io/doc/org.mockito/mockito-core/5.14.2/org/mockito/Mockito.html)
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/index.html)
- [JaCoCo Maven plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [PIT Maven configuration](https://pitest.org/quickstart/maven/)
- [PIT JUnit 5 adapter](https://github.com/pitest/pitest-junit5-plugin)
