package dev.vfyjxf.mcp.registrar;

import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;
import dev.vfyjxf.mcp.api.registrar.CommonMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpToolRegistrar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AnnotationMcpRegistrarLookupTest {

    @Test
    void findsAnnotatedRegistrarImplementations() {
        var lookup = new AnnotationMcpRegistrarLookup<>(
                CommonMcpToolRegistrar.class,
                () -> List.of(AnnotatedCommonRegistrar.class.getName())
        );

        var registrars = lookup.findRegistrars();

        assertEquals(1, registrars.size());
        assertInstanceOf(AnnotatedCommonRegistrar.class, registrars.iterator().next());
    }

    @Test
    void ignoresAnnotatedClassesThatDoNotImplementRegistrarInterface() {
        var lookup = new AnnotationMcpRegistrarLookup<>(
                CommonMcpToolRegistrar.class,
                () -> List.of(WrongAnnotatedType.class.getName())
        );

        assertEquals(0, lookup.findRegistrars().size());
    }

    @CommonMcpRegistrar
    public static final class AnnotatedCommonRegistrar implements CommonMcpToolRegistrar {
        @Override
        public void register(RegisterCommonMcpToolsEvent event) {
        }
    }

    @CommonMcpRegistrar
    public static final class WrongAnnotatedType {
    }
}
