package airplane.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandingGearTest {
    @Test
    void gear_StartsDeployedAndCanRetractAndDeployAgain() {
        LandingGear gear = new LandingGear();
        assertTrue(gear.isDeployed());

        gear.retract();
        assertFalse(gear.isDeployed());
        gear.deploy();
        assertTrue(gear.isDeployed());
    }
}
