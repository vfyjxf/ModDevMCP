package dev.vfyjxf.moddev.api.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetSelectorTest {

    @Test
    void selectorCanRepresentIncludeAndExcludeFilters() {
        var selector = TargetSelector.builder()
                .scope("screen")
                .modId("jei")
                .text("Search")
                .exclude(List.of(TargetSelector.builder().role("tooltip").build()))
                .build();

        assertEquals("jei", selector.modId());
        assertEquals(1, selector.exclude().size());
    }
}

