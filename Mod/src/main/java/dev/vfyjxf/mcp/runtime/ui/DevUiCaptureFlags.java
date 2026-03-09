package dev.vfyjxf.mcp.runtime.ui;

final class DevUiCaptureFlags {

    static final String DEV_UI_CAPTURE_PROPERTY = "moddevmcp.devUiCapture";

    private DevUiCaptureFlags() {
    }

    static boolean isEnabledBySystemProperty() {
        return Boolean.parseBoolean(System.getProperty(DEV_UI_CAPTURE_PROPERTY, "false"));
    }

    static boolean shouldAttach(boolean productionEnvironment) {
        return !productionEnvironment && isEnabledBySystemProperty();
    }
}
