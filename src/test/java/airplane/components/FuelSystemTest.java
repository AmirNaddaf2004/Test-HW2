package airplane.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FuelSystemTest {
    @ParameterizedTest
    @CsvSource({"0, 0, true", "100, 100, true", "99.5, 100, false", "100.5, 100, true"})
    void hasEnoughFuel_IncludesTheExactTakeoffThreshold(double initial, double minimum, boolean expected) {
        FuelSystem fuel = new FuelSystem(initial, minimum);

        assertEquals(initial, fuel.getRemainingFuel());
        assertEquals(expected, fuel.hasEnoughFuel());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-Double.MIN_VALUE, -1.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void constructor_RejectsInvalidInitialFuel(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new FuelSystem(invalid, 10.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-Double.MIN_VALUE, -1.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void constructor_RejectsInvalidTakeoffMinimum(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new FuelSystem(100.0, invalid));
    }

    @Test
    void consumeFuel_SubtractsEachAmountAndUpdatesReadiness() {
        FuelSystem fuel = new FuelSystem(100.0, 50.0);

        fuel.consumeFuel(40.0);
        assertEquals(60.0, fuel.getRemainingFuel());
        assertTrue(fuel.hasEnoughFuel());
        fuel.consumeFuel(20.0);
        assertEquals(40.0, fuel.getRemainingFuel());
        assertFalse(fuel.hasEnoughFuel());
    }

    @Test
    void consumeFuel_AllowsConsumingExactlyTheRemainingFuel() {
        FuelSystem fuel = new FuelSystem(100.0, 10.0);

        fuel.consumeFuel(100.0);

        assertEquals(0.0, fuel.getRemainingFuel());
        assertFalse(fuel.hasEnoughFuel());
    }

    @Test
    void consumeFuel_AllowsTheSmallestPositiveAmount() {
        FuelSystem fuel = new FuelSystem(Double.MIN_VALUE, 0.0);

        fuel.consumeFuel(Double.MIN_VALUE);

        assertEquals(0.0, fuel.getRemainingFuel());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, -Double.MIN_VALUE, -1.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void consumeFuel_RejectsInvalidAmountsWithoutChangingFuel(double invalid) {
        FuelSystem fuel = new FuelSystem(100.0, 10.0);

        assertThrows(IllegalArgumentException.class, () -> fuel.consumeFuel(invalid));

        assertEquals(100.0, fuel.getRemainingFuel());
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.01", "100.0, 100.01"})
    void consumeFuel_RejectsInsufficientFuelWithoutChangingIt(double initial, double amount) {
        FuelSystem fuel = new FuelSystem(initial, 0.0);

        assertThrows(IllegalStateException.class, () -> fuel.consumeFuel(amount));

        assertEquals(initial, fuel.getRemainingFuel());
    }
}
