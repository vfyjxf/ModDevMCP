package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.ui.UiTarget;

final class VanillaWidgetPressSupport {

    private VanillaWidgetPressSupport() {
    }

    static boolean invokeButtonPress(Object screen, UiTarget target) {
        if (screen == null || target == null || !"button".equals(target.role())) {
            return false;
        }
        for (Object child : VanillaWidgetIntrospection.collectWidgets(screen)) {
            if (!VanillaWidgetIntrospection.matchesTarget(child, target)) {
                continue;
            }
            if (invokeNoArg(child, "onPress")) {
                return true;
            }
        }
        return false;
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
}

