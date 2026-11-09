package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Complements isolated Mockito tests with the actual component implementations. */
class AircraftIntegrationTest {
    private Engine engine;
    private FuelSystem fuel;
    private LandingGear gear;
    private NavigationSystem navigation;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = new Engine();
        fuel = new FuelSystem(200.0, 50.0);
        gear = new LandingGear();
        navigation = new NavigationSystem("Tehran");
        aircraft = new Aircraft(engine, fuel, gear, navigation);
    }

    @Test
    void normalFlights_UpdateComponentsAndAllowAnotherFlightAfterLanding() {
        fly();
        assertTrue(engine.isRunning());
        assertFalse(gear.isDeployed());
        aircraft.cruise(50.0);
        aircraft.cruise(30.0);
        assertEquals(120.0, fuel.getRemainingFuel());
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        aircraft.land();
        assertLanded();

        fly();
        aircraft.cruise(120.0);
        aircraft.land();

        assertLanded();
        assertEquals(0.0, fuel.getRemainingFuel());
        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
    }

    @Test
    void engineFailureDuringFlight_LandsWithoutSpendingMoreFuel() {
        fly();
        aircraft.cruise(25.0);
        engine.fail();

        aircraft.cruise(25.0);

        assertLanded();
        assertEquals(175.0, fuel.getRemainingFuel());
        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
    }

    @Test
    void insufficientCruiseFuel_LandsAndReportsTheOriginalFailure() {
        fly();

        FlightException failure = assertThrows(FlightException.class, () -> aircraft.cruise(201.0));

        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals(200.0, fuel.getRemainingFuel());
        assertLanded();
    }

    @Test
    void routeCanBecomeAvailableAfterFailedPreparation() {
        navigation.setRouteAvailable(false);
        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        assertFalse(engine.isRunning());

        navigation.setRouteAvailable(true);
        fly();
        aircraft.emergencyLand();

        assertLanded();
    }

    @Test
    void engineFailureAfterPreparation_AbortsTakeoffWithoutRetractingGear() {
        aircraft.prepareForTakeoff();
        engine.fail();

        FlightException failure = assertThrows(FlightException.class, aircraft::takeOff);

        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(gear.isDeployed());
    }

    @Test
    void invalidFuelInput_DoesNotCorruptTheFlightOrFuel() {
        fly();

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(Double.NaN));

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        assertEquals(200.0, fuel.getRemainingFuel());
        assertTrue(engine.isRunning());
        assertFalse(gear.isDeployed());
        aircraft.cruise(10.0);
        assertEquals(190.0, fuel.getRemainingFuel());
    }

    private void fly() {
        aircraft.prepareForTakeoff();
        assertEquals(FlightStatus.READY, aircraft.getStatus());
        aircraft.takeOff();
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
    }

    private void assertLanded() {
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertTrue(gear.isDeployed());
        assertFalse(engine.isRunning());
    }
}
