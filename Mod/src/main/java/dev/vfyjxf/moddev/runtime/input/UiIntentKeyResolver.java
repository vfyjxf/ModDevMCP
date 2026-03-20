package dev.vfyjxf.moddev.runtime.input;

@FunctionalInterface
interface UiIntentKeyResolver {

    int resolve(String intent);
}

