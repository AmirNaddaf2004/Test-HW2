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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AircraftTest {

    private static final double VALID_CRUISE_FUEL_AMOUNT = 50.0;
    private static final double SECOND_CRUISE_FUEL_AMOUNT = 30.0;

    private Engine engineMock;
    private FuelSystem fuelSystemMock;
    private LandingGear landingGearMock;
    private NavigationSystem navigationSystemMock;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engineMock = mock(Engine.class);
        fuelSystemMock = mock(FuelSystem.class);
        landingGearMock = mock(LandingGear.class);
        navigationSystemMock = mock(NavigationSystem.class);

        aircraft = new Aircraft(
                engineMock,
                fuelSystemMock,
                landingGearMock,
                navigationSystemMock
        );
    }

    private void configureAllComponentsReadyForTakeoff() {
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(true);
        when(navigationSystemMock.isRouteAvailable()).thenReturn(true);
    }

    private void transitionAircraftToInFlightState() {
        configureAllComponentsReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
        reset(engineMock, fuelSystemMock, landingGearMock, navigationSystemMock);
    }

    // =========================================================================
    // 1. Tests for prepareForTakeoff()
    // =========================================================================

    @Test
    void prepareForTakeoff_AllComponentsOperationalAndReady_TransitionsStatusToReady() {
        configureAllComponentsReadyForTakeoff();

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engineMock, times(1)).isOperational();
        verify(fuelSystemMock, times(1)).hasEnoughFuel();
        verify(navigationSystemMock, times(1)).isRouteAvailable();
    }

    @Test
    void prepareForTakeoff_WhenEngineIsNotOperational_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(fuelSystemMock, times(0)).hasEnoughFuel();
    }

    @Test
    void prepareForTakeoff_WhenFuelIsInsufficient_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(navigationSystemMock, times(0)).isRouteAvailable();
    }

    @Test
    void prepareForTakeoff_WhenRouteIsUnavailable_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(true);
        when(navigationSystemMock.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
    }

    @Test
    void prepareForTakeoff_WhenAlreadyInFlight_ThrowsFlightException() {
        transitionAircraftToInFlightState();

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
    }

    // =========================================================================
    // 2. Tests for takeOff()
    // =========================================================================

    @Test
    void takeOff_WhenAircraftIsReady_StartsEngineRetractsGearAndEntersInFlight() {
        configureAllComponentsReadyForTakeoff();
        aircraft.prepareForTakeoff();
        doNothing().when(engineMock).start();
        doNothing().when(landingGearMock).retract();

        aircraft.takeOff();

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(engineMock, times(1)).start();
        verify(landingGearMock, times(1)).retract();
    }

    @Test
    void takeOff_WhenEngineStartThrowsException_AbortsFlightAndThrowsFlightException() {
        configureAllComponentsReadyForTakeoff();
        aircraft.prepareForTakeoff();
        doThrow(new IllegalStateException("The engine is not operational.")).when(engineMock).start();

        FlightException thrown = assertThrows(FlightException.class, () -> aircraft.takeOff());

        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        verify(landingGearMock, times(0)).retract();
    }

    @Test
    void takeOff_WhenNotReady_ThrowsFlightExceptionWithoutStartingEngine() {
        assertThrows(FlightException.class, () -> aircraft.takeOff());
        verify(engineMock, times(0)).start();
    }

    // =========================================================================
    // 3. Tests for cruise()
    // =========================================================================

    @Test
    void cruise_WhenEngineIsOperationalAndFuelSufficient_ConsumesFuelAndStaysInFlight() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenReturn(true);
        doNothing().when(fuelSystemMock).consumeFuel(any(Double.class));

        aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT);
        aircraft.cruise(SECOND_CRUISE_FUEL_AMOUNT);

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystemMock, times(2)).consumeFuel(any(Double.class));
        verify(fuelSystemMock, times(1)).consumeFuel(VALID_CRUISE_FUEL_AMOUNT);
        verify(fuelSystemMock, times(1)).consumeFuel(SECOND_CRUISE_FUEL_AMOUNT);
    }

    @Test
    void cruise_WhenEngineReportsFailureMidFlight_PerformsEmergencyLandingWithoutConsumingFuel() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenReturn(false);

        aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGearMock, times(1)).deploy();
        verify(engineMock, times(1)).stop();
        verify(fuelSystemMock, times(0)).consumeFuel(any(Double.class));
    }

    @Test
    void cruise_WhenEngineCheckThrowsRuntimeExceptionInCruise_PropagatesException() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenThrow(new IllegalStateException("Engine sensor hardware fault"));

        assertThrows(IllegalStateException.class, () -> aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT));
        verify(fuelSystemMock, times(0)).consumeFuel(any(Double.class));
    }

    @Test
    void cruise_WhenFuelConsumptionThrowsException_PerformsEmergencyLandingAndThrowsFlightException() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenReturn(true);
        doThrow(new IllegalStateException("Not enough fuel."))
                .when(fuelSystemMock).consumeFuel(any(Double.class));

        assertThrows(FlightException.class, () -> aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT));

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGearMock, times(1)).deploy();
        verify(engineMock, times(1)).stop();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -10.5})
    void cruise_WhenFuelAmountIsNonPositive_ThrowsIllegalArgumentException(double invalidFuelAmount) {
        transitionAircraftToInFlightState();

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(invalidFuelAmount));
        verify(fuelSystemMock, times(0)).consumeFuel(any(Double.class));
    }

    // =========================================================================
    // 4. Tests for land() and Re-Preparing After Landing
    // =========================================================================

    @Test
    void land_WhenInFlight_DeploysLandingGearStopsEngineAndSetsStatusToLanded() {
        transitionAircraftToInFlightState();

        aircraft.land();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGearMock, times(1)).deploy();
        verify(engineMock, times(1)).stop();
    }

    @Test
    void prepareForTakeoff_WhenAircraftHasLanded_AllowsRePreparationToReadyStatus() {
        transitionAircraftToInFlightState();
        aircraft.land();
        reset(engineMock, fuelSystemMock, landingGearMock, navigationSystemMock);
        configureAllComponentsReadyForTakeoff();

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engineMock, times(1)).isOperational();
    }
}