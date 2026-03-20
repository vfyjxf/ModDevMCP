package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;

import java.lang.reflect.Method;
import java.util.*;

final class VanillaWidgetIntrospection {

    private VanillaWidgetIntrospection() {
    }

    static List<UiTarget> extractTargets(Object root, UiContext context, String driverId) {
        var widgets = collectWidgets(root);
        var targets = new ArrayList<UiTarget>(widgets.size());
        for (var index = 0; index < widgets.size(); index++) {
            var widget = widgets.get(index);
            targets.add(new UiTarget(
                    targetId(widget, index),
                    driverId,
                    context.screenClass(),
                    context.modId(),
                    "button",
                    widgetText(widget),
                    new Bounds(widgetX(widget), widgetY(widget), widgetWidth(widget), widgetHeight(widget)),
                    new UiTargetState(
                            widgetVisible(widget),
                            widgetActive(widget),
                            widgetFocused(widget),
                            false,
                            false,
                            false
                    ),
                    List.of("click", "hover", "focus"),
                    Map.of(
                            "widgetClass", widget.getClass().getName()
                    )
            ));
        }
        return List.copyOf(targets);
    }

    static List<Object> collectWidgets(Object root) {
        var visited = new IdentityHashMap<Object, Boolean>();
        var widgets = new ArrayList<Object>();
        collect(root, true, visited, widgets);
        return List.copyOf(widgets);
    }

    static boolean matchesTarget(Object widget, UiTarget target) {
        if (widget == null || target == null) {
            return false;
        }
        return widgetX(widget) == target.bounds().x()
                && widgetY(widget) == target.bounds().y()
                && widgetWidth(widget) == target.bounds().width()
                && widgetHeight(widget) == target.bounds().height()
                && Objects.equals(target.text(), widgetText(widget));
    }

    private static void collect(Object current, boolean root, IdentityHashMap<Object, Boolean> visited, List<Object> widgets) {
        if (current == null || visited.put(current, Boolean.TRUE) != null) {
            return;
        }
        if (!root && isExtractableWidget(current)) {
            widgets.add(current);
        }
        for (var child : children(current)) {
            collect(child, false, visited, widgets);
        }
    }

    private static boolean isExtractableWidget(Object candidate) {
        return hasNoArgMethod(candidate, "getX")
                && hasNoArgMethod(candidate, "getY")
                && hasNoArgMethod(candidate, "getWidth")
                && hasNoArgMethod(candidate, "getHeight")
                && hasNoArgMethod(candidate, "getMessage");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> children(Object current) {
        var children = invoke(current, "children");
        if (children instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private static boolean hasNoArgMethod(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static String targetId(Object widget, int index) {
        var text = widgetText(widget);
        var normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (!normalized.isBlank()) {
            return "button-" + normalized;
        }
        return "button-" + index;
    }

    static String widgetText(Object widget) {
        var message = invoke(widget, "getMessage");
        if (message == null) {
            return "";
        }
        return stringValue(message);
    }

    static int widgetX(Object widget) {
        return intMethod(widget, "getX");
    }

    static int widgetY(Object widget) {
        return intMethod(widget, "getY");
    }

    static int widgetWidth(Object widget) {
        return intMethod(widget, "getWidth");
    }

    static int widgetHeight(Object widget) {
        return intMethod(widget, "getHeight");
    }

    static boolean widgetVisible(Object widget) {
        return booleanField(widget, "visible", true);
    }

    static boolean widgetActive(Object widget) {
        return booleanField(widget, "active", true);
    }

    static boolean widgetFocused(Object widget) {
        try {
            var isFocused = widget.getClass().getMethod("isFocused");
            return Boolean.TRUE.equals(isFocused.invoke(widget));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int intMethod(Object target, String methodName) {
        var value = invoke(target, methodName);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean booleanField(Object target, String fieldName, boolean defaultValue) {
        try {
            var field = target.getClass().getField(fieldName);
            return field.getBoolean(target);
        } catch (ReflectiveOperationException ignored) {
            return defaultValue;
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
            return raw == null ? "" : String.valueOf(raw);
        } catch (ReflectiveOperationException ignored) {
            return String.valueOf(value);
        }
    }
}

