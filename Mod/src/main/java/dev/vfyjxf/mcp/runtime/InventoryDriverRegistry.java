package dev.vfyjxf.mcp.runtime;

import dev.vfyjxf.mcp.api.runtime.InventoryContext;
import dev.vfyjxf.mcp.api.runtime.InventoryDriver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InventoryDriverRegistry {

    private final List<InventoryDriver> drivers = new ArrayList<>();

    public void register(InventoryDriver driver) {
        drivers.add(driver);
        drivers.sort(Comparator.comparingInt((InventoryDriver driver1) -> driver1.descriptor().priority()).reversed());
    }

    public int size() {
        return drivers.size();
    }

    public List<InventoryDriver> all() {
        return List.copyOf(drivers);
    }

    public Optional<InventoryDriver> select(InventoryContext context) {
        return drivers.stream().filter(driver -> driver.matches(context)).findFirst();
    }
}
