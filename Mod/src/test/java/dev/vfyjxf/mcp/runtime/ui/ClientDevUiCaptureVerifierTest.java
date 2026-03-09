package dev.vfyjxf.mcp.runtime.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientDevUiCaptureVerifierTest {

    @Test
    void autoCaptureIsDisabledByDefault() {
        System.clearProperty(DevUiCaptureFlags.DEV_UI_CAPTURE_PROPERTY);

        assertFalse(DevUiCaptureFlags.isEnabledBySystemProperty());
        assertFalse(DevUiCaptureFlags.shouldAttach(false));
    }

    @Test
    void autoCaptureRequiresDevEnvironmentAndExplicitProperty() {
        var previous = System.getProperty(DevUiCaptureFlags.DEV_UI_CAPTURE_PROPERTY);
        System.setProperty(DevUiCaptureFlags.DEV_UI_CAPTURE_PROPERTY, "true");
        try {
            assertTrue(DevUiCaptureFlags.isEnabledBySystemProperty());
            assertTrue(DevUiCaptureFlags.shouldAttach(false));
            assertFalse(DevUiCaptureFlags.shouldAttach(true));
        } finally {
            restore(previous);
        }
    }

    private void restore(String value) {
        if (value == null) {
            System.clearProperty(DevUiCaptureFlags.DEV_UI_CAPTURE_PROPERTY);
            return;
        }
        System.setProperty(DevUiCaptureFlags.DEV_UI_CAPTURE_PROPERTY, value);
    }
}
