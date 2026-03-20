package dev.vfyjxf.moddev.registrar;

import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.CommonOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonRegistrar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AnnotationRegistrarLookupTest {

    @Test
    void findsAnnotatedRegistrarImplementations() {
        var lookup = new AnnotationRegistrarLookup<>(
                CommonOperationRegistrar.class,
                () -> List.of(AnnotatedCommonRegistrar.class.getName())
        );

        var registrars = lookup.findRegistrars();

        assertEquals(1, registrars.size());
        assertInstanceOf(AnnotatedCommonRegistrar.class, registrars.iterator().next());
    }

    @Test
    void ignoresAnnotatedClassesThatDoNotImplementRegistrarInterface() {
        var lookup = new AnnotationRegistrarLookup<>(
                CommonOperationRegistrar.class,
                () -> List.of(WrongAnnotatedType.class.getName())
        );

        assertEquals(0, lookup.findRegistrars().size());
    }

    @CommonRegistrar
    public static final class AnnotatedCommonRegistrar implements CommonOperationRegistrar {
        @Override
        public void register(RegisterCommonOperationsEvent event) {
        }
    }

    @CommonRegistrar
    public static final class WrongAnnotatedType {
    }
}
