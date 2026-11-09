package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AircraftStateTest {
    private Engine engine;
    private FuelSystem fuel;
    private LandingGear gear;
    private NavigationSystem navigation;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = mock(Engine.class);
        fuel = mock(FuelSystem.class);
        gear = mock(LandingGear.class);
        navigation = mock(NavigationSystem.class);
        aircraft = new Aircraft(engine, fuel, gear, navigation);
    }

    @Test
    void constructor_StartsGroundedWithoutUsingDependencies() {
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(engine, fuel, gear, navigation);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void constructor_RejectsEachMissingDependency(int missing) {
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> new Aircraft(missing == 0 ? null : engine, missing == 1 ? null : fuel,
                        missing == 2 ? null : gear, missing == 3 ? null : navigation));

        String[] names = {"engine", "fuelSystem", "landingGear", "navigationSystem"};
        assertEquals(names[missing] + " must not be null", failure.getMessage());
        verifyNoInteractions(engine, fuel, gear, navigation);
    }

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("invalidOperations")
    void invalidOperation_PreservesStateAndDoesNotTouchComponents(FlightStatus status, Operation operation) {
        reach(status);

        assertThrows(FlightException.class, () -> operation.execute(aircraft));

        assertEquals(status, aircraft.getStatus());
        verifyNoInteractions(engine, fuel, gear, navigation);
    }

    static Stream<Arguments> invalidOperations() {
        return Stream.of(FlightStatus.values()).flatMap(status -> Stream.of(Operation.values())
                .filter(operation -> !operation.allowedIn(status))
                .map(operation -> Arguments.of(status, operation)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void prepare_WhenReadinessCheckThrows_PropagatesCauseAndPreservesGrounded(int failingCheck) {
        when(engine.isOperational()).thenReturn(true);
        when(fuel.hasEnoughFuel()).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("Readiness sensor failed");
        switch (failingCheck) {
            case 0 -> when(engine.isOperational()).thenThrow(failure);
            case 1 -> when(fuel.hasEnoughFuel()).thenThrow(failure);
            case 2 -> when(navigation.isRouteAvailable()).thenThrow(failure);
            default -> throw new AssertionError("Unknown check");
        }

        assertSame(failure, assertThrows(IllegalStateException.class, aircraft::prepareForTakeoff));
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(gear);
        verify(engine, never()).start();
        if (failingCheck == 0) {
            verifyNoInteractions(fuel, navigation);
        } else if (failingCheck == 1) {
            verifyNoInteractions(navigation);
        }
    }

    @Test
    void prepare_WhenLandedButRouteUnavailable_RemainsLanded() {
        reach(FlightStatus.LANDED);
        when(navigation.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engine, never()).start();
        verifyNoInteractions(gear);
    }

    @ParameterizedTest
    @EnumSource(value = FlightStatus.class, names = {"GROUNDED", "READY", "LANDED", "ABORTED"})
    void cruise_WhenStateAndAmountAreInvalid_RejectsStateFirst(FlightStatus status) {
        reach(status);

        assertThrows(FlightException.class, () -> aircraft.cruise(0.0));

        assertEquals(status, aircraft.getStatus());
        verifyNoInteractions(engine, fuel, gear, navigation);
    }

    private void reach(FlightStatus target) {
        if (target != FlightStatus.GROUNDED) {
            when(engine.isOperational()).thenReturn(true);
            when(fuel.hasEnoughFuel()).thenReturn(true);
            when(navigation.isRouteAvailable()).thenReturn(true);
            aircraft.prepareForTakeoff();
            if (target == FlightStatus.ABORTED) {
                doThrow(new IllegalStateException("Engine failed to start")).when(engine).start();
                assertThrows(FlightException.class, aircraft::takeOff);
            } else if (target != FlightStatus.READY) {
                aircraft.takeOff();
                if (target == FlightStatus.LANDED) {
                    aircraft.land();
                }
            }
        }
        assertEquals(target, aircraft.getStatus());
        clearInvocations(engine, fuel, gear, navigation);
    }

    enum Operation {
        PREPARE, TAKE_OFF, CRUISE, LAND, EMERGENCY_LAND;

        boolean allowedIn(FlightStatus status) {
            return switch (this) {
                case PREPARE -> status == FlightStatus.GROUNDED || status == FlightStatus.LANDED;
                case TAKE_OFF -> status == FlightStatus.READY;
                case CRUISE, LAND, EMERGENCY_LAND -> status == FlightStatus.IN_FLIGHT;
            };
        }

        void execute(Aircraft aircraft) {
            switch (this) {
                case PREPARE -> aircraft.prepareForTakeoff();
                case TAKE_OFF -> aircraft.takeOff();
                case CRUISE -> aircraft.cruise(10.0);
                case LAND -> aircraft.land();
                case EMERGENCY_LAND -> aircraft.emergencyLand();
            }
        }
    }
}
