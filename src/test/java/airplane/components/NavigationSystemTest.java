package airplane.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NavigationSystemTest {
    @Test
    void defaultRoute_IsAvailableAndRetainsDestination() {
        NavigationSystem navigation = new NavigationSystem("Tehran");

        assertTrue(navigation.isRouteAvailable());
        assertEquals("Tehran", navigation.getDestination());
    }

    @Test
    void routeAvailability_CanChangeInBothDirections() {
        NavigationSystem navigation = new NavigationSystem("Shiraz", false);
        assertFalse(navigation.isRouteAvailable());

        navigation.setRouteAvailable(true);
        assertTrue(navigation.isRouteAvailable());
        navigation.setRouteAvailable(false);
        assertFalse(navigation.isRouteAvailable());
        assertEquals("Shiraz", navigation.getDestination());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void constructor_RejectsMissingOrBlankDestination(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new NavigationSystem(invalid));
        assertThrows(IllegalArgumentException.class, () -> new NavigationSystem(invalid, false));
    }
}
