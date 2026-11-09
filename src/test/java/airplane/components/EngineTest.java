package airplane.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    @Test
    void defaultEngine_IsOperationalAndStopped() {
        Engine engine = new Engine();

        assertTrue(engine.isOperational());
        assertFalse(engine.isRunning());
    }

    @Test
    void startAndStop_ChangeRunningStateWithoutChangingHealth() {
        Engine engine = new Engine(true);

        engine.start();
        assertTrue(engine.isRunning());
        engine.stop();
        assertFalse(engine.isRunning());
        assertTrue(engine.isOperational());
    }

    @Test
    void nonOperationalEngine_CannotStart() {
        Engine engine = new Engine(false);

        assertFalse(engine.isOperational());
        assertThrows(IllegalStateException.class, engine::start);
        assertFalse(engine.isRunning());
    }

    @Test
    void fail_StopsARunningEngineAndPreventsRestart() {
        Engine engine = new Engine();
        engine.start();

        engine.fail();

        assertFalse(engine.isOperational());
        assertFalse(engine.isRunning());
        assertThrows(IllegalStateException.class, engine::start);
    }
}
