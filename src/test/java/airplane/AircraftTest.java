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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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
        // Keep the stubs; only forget setup interactions before the action under test.
        clearInvocations(engineMock, fuelSystemMock, landingGearMock, navigationSystemMock);
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
        verifyNoInteractions(landingGearMock);
        verifyNoMoreInteractions(engineMock, fuelSystemMock, navigationSystemMock);
    }

    @Test
    void prepareForTakeoff_WhenEngineIsNotOperational_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engineMock, times(1)).isOperational();
        verifyNoInteractions(fuelSystemMock, navigationSystemMock, landingGearMock);
    }

    @Test
    void prepareForTakeoff_WhenFuelIsInsufficient_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(navigationSystemMock, times(0)).isRouteAvailable();
        verifyNoInteractions(navigationSystemMock, landingGearMock);
        verify(engineMock, never()).start();
    }

    @Test
    void prepareForTakeoff_WhenRouteIsUnavailable_ThrowsFlightExceptionAndRemainsGrounded() {
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(true);
        when(navigationSystemMock.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(landingGearMock);
        verify(engineMock, never()).start();
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
        InOrder order = inOrder(engineMock, landingGearMock);
        order.verify(engineMock).start();
        order.verify(landingGearMock).retract();
    }

    @Test
    void takeOff_WhenEngineStartThrowsException_AbortsFlightAndThrowsFlightException() {
        configureAllComponentsReadyForTakeoff();
        aircraft.prepareForTakeoff();
        IllegalStateException failure = new IllegalStateException("The engine is not operational.");
        doThrow(failure).when(engineMock).start();

        FlightException thrown = assertThrows(FlightException.class, () -> aircraft.takeOff());

        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        assertSame(failure, thrown.getCause());
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
        aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT);
        aircraft.cruise(SECOND_CRUISE_FUEL_AMOUNT);

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystemMock, times(2)).consumeFuel(any(Double.class));
        verify(fuelSystemMock, times(1)).consumeFuel(VALID_CRUISE_FUEL_AMOUNT);
        verify(fuelSystemMock, times(1)).consumeFuel(SECOND_CRUISE_FUEL_AMOUNT);
        ArgumentCaptor<Double> amounts = ArgumentCaptor.forClass(Double.class);
        verify(fuelSystemMock, times(2)).consumeFuel(amounts.capture());
        assertEquals(java.util.List.of(50.0, 30.0), amounts.getAllValues());
        verifyNoInteractions(landingGearMock, navigationSystemMock);
        verify(engineMock, never()).stop();
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
        InOrder order = inOrder(engineMock, landingGearMock);
        order.verify(engineMock).isOperational();
        order.verify(landingGearMock).deploy();
        order.verify(engineMock).stop();
    }

    @Test
    void cruise_WhenEngineCheckThrowsRuntimeExceptionInCruise_PropagatesException() {
        transitionAircraftToInFlightState();
        IllegalStateException failure = new IllegalStateException("Engine sensor hardware fault");
        when(engineMock.isOperational()).thenThrow(failure);

        assertSame(failure,
                assertThrows(IllegalStateException.class, () -> aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT)));
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verifyNoInteractions(landingGearMock);
        verify(engineMock, never()).stop();
        verify(fuelSystemMock, times(0)).consumeFuel(any(Double.class));
    }

    @Test
    void cruise_WhenFuelConsumptionThrowsException_PerformsEmergencyLandingAndThrowsFlightException() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("Not enough fuel.");
        doThrow(failure)
                .when(fuelSystemMock).consumeFuel(any(Double.class));

        FlightException thrown = assertThrows(FlightException.class,
                () -> aircraft.cruise(VALID_CRUISE_FUEL_AMOUNT));
        assertSame(failure, thrown.getCause());

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGearMock, times(1)).deploy();
        verify(engineMock, times(1)).stop();
        InOrder order = inOrder(fuelSystemMock, landingGearMock, engineMock);
        order.verify(fuelSystemMock).consumeFuel(VALID_CRUISE_FUEL_AMOUNT);
        order.verify(landingGearMock).deploy();
        order.verify(engineMock).stop();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, -Double.MIN_VALUE, -10.5, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void cruise_WhenFuelAmountIsInvalid_ThrowsIllegalArgumentException(double invalidFuelAmount) {
        transitionAircraftToInFlightState();

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(invalidFuelAmount));
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verifyNoInteractions(engineMock, fuelSystemMock, landingGearMock, navigationSystemMock);
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
        InOrder order = inOrder(landingGearMock, engineMock);
        order.verify(landingGearMock).deploy();
        order.verify(engineMock).stop();
        verifyNoMoreInteractions(landingGearMock, engineMock);
        verifyNoInteractions(fuelSystemMock, navigationSystemMock);
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

    @Test
    void takeOff_WhenGearRetractionFails_AbortsAndPreservesCause() {
        configureAllComponentsReadyForTakeoff();
        aircraft.prepareForTakeoff();
        IllegalStateException failure = new IllegalStateException("Gear is jammed");
        doThrow(failure).when(landingGearMock).retract();

        FlightException thrown = assertThrows(FlightException.class, aircraft::takeOff);

        assertSame(failure, thrown.getCause());
        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        InOrder order = inOrder(engineMock, landingGearMock);
        order.verify(engineMock).start();
        order.verify(landingGearMock).retract();
    }

    @Test
    void emergencyLand_WhenInFlight_DeploysGearBeforeStoppingEngine() {
        transitionAircraftToInFlightState();

        aircraft.emergencyLand();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        InOrder order = inOrder(landingGearMock, engineMock);
        order.verify(landingGearMock).deploy();
        order.verify(engineMock).stop();
        verifyNoMoreInteractions(landingGearMock, engineMock);
        verifyNoInteractions(fuelSystemMock, navigationSystemMock);
    }

    @Test
    void cruise_WhenEngineFailsAfterOneSuccessfulCruise_LandsWithoutConsumingMoreFuel() {
        transitionAircraftToInFlightState();
        when(engineMock.isOperational()).thenReturn(true, false);

        aircraft.cruise(50.0);
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        aircraft.cruise(30.0);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engineMock, times(2)).isOperational();
        verify(fuelSystemMock).consumeFuel(50.0);
        verifyNoMoreInteractions(fuelSystemMock);
        verify(landingGearMock).deploy();
        verify(engineMock).stop();
    }

    @Test
    void reset_RemovesStubsAndInteractionsBeforeReconfiguringTheSameMock() {
        when(engineMock.isOperational()).thenReturn(true);
        assertTrue(engineMock.isOperational());
        engineMock.start();

        // reset() removes both stubbing and invocation history, unlike clearInvocations().
        reset(engineMock);
        verifyNoInteractions(engineMock);
        assertFalse(engineMock.isOperational());
        when(engineMock.isOperational()).thenReturn(true);
        when(fuelSystemMock.hasEnoughFuel()).thenReturn(true);
        when(navigationSystemMock.isRouteAvailable()).thenReturn(true);
        clearInvocations(engineMock);

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engineMock, times(1)).isOperational();
        verify(engineMock, never()).start();
    }
}
