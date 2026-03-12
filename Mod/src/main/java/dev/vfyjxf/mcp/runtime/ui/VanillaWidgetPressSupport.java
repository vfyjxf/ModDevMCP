package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

final class VanillaWidgetPressSupport {

    private VanillaWidgetPressSupport() {
    }

    static boolean invokeButtonPress(Object screen, UiTarget target) {
        if (screen == null || target == null || !"button".equals(target.role())) {
            return false;
        }
        for (Object child : children(screen)) {
            if (!matchesTarget(child, target)) {
                continue;
            }
            if (invokeNoArg(child, "onPress")) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesTarget(Object child, UiTarget target) {
        if (child == null) {
            return false;
        }
        return intMethod(child, "getX", Integer.MIN_VALUE) == target.bounds().x()
                && intMethod(child, "getY", Integer.MIN_VALUE) == target.bounds().y()
                && intMethod(child, "getWidth", Integer.MIN_VALUE) == target.bounds().width()
                && intMethod(child, "getHeight", Integer.MIN_VALUE) == target.bounds().height()
                && messageMatches(child, target.text());
    }

    private static boolean messageMatches(Object child, String expectedText) {
        if (expectedText == null || expectedText.isBlank()) {
            return true;
        }
        var message = invoke(child, "getMessage");
        if (message == null) {
            return false;
        }
        var extracted = stringValue(message);
        return Objects.equals(expectedText, extracted);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> children(Object screen) {
        var children = invoke(screen, "children");
        if (children instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private static int intMethod(Object target, String methodName, int defaultValue) {
        var value = invoke(target, methodName);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private static boolean invokeNoArg(Object target, String methodName) {
        try {
            var method = target.getClass().getMethod(methodName);
            method.invoke(target);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            var method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        try {
            Method getString = value.getClass().getMethod("getString");
            var raw = getString.invoke(value);
            return raw == null ? null : String.valueOf(raw);
        } catch (ReflectiveOperationException ignored) {
            return String.valueOf(value);
        }
    }
}
