package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UiDriverRegistry {

    private final List<UiDriver> drivers = new ArrayList<>();

    public void register(UiDriver driver) {
        drivers.add(driver);
        drivers.sort(Comparator.comparingInt((UiDriver driver1) -> driver1.descriptor().priority()).reversed());
    }

    public int size() {
        return drivers.size();
    }

    public List<UiDriver> all() {
        return List.copyOf(drivers);
    }

    public List<UiDriver> matchingDrivers(UiContext context) {
        return drivers.stream()
                .filter(driver -> driver.matches(context))
                .toList();
    }

    public Optional<UiDriver> select(UiContext context) {
        return matchingDrivers(context).stream().findFirst();
    }
}

