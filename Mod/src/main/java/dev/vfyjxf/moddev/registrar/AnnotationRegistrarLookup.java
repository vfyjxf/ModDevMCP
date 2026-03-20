package dev.vfyjxf.moddev.registrar;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class AnnotationRegistrarLookup<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationRegistrarLookup.class);

    private final Class<T> registrarClass;
    private final Supplier<? extends Collection<String>> classNameSupplier;

    public AnnotationRegistrarLookup(Class<T> registrarClass, Class<? extends Annotation> annotationClass) {
        this(registrarClass, classNamesFor(annotationClass));
    }

    AnnotationRegistrarLookup(Class<T> registrarClass, Supplier<? extends Collection<String>> classNameSupplier) {
        this.registrarClass = Objects.requireNonNull(registrarClass, "registrarClass");
        this.classNameSupplier = Objects.requireNonNull(classNameSupplier, "classNameSupplier");
    }

    public Collection<T> findRegistrars() {
        List<T> registrars = new ArrayList<>();
        for (String className : classNameSupplier.get()) {
            try {
                Class<?> candidate = Class.forName(className);
                if (!registrarClass.isAssignableFrom(candidate)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                T registrar = (T) candidate.getConstructor().newInstance();
                registrars.add(registrar);
            } catch (ClassNotFoundException exception) {
                LOGGER.error("Registrar class not found: {}", className, exception);
            } catch (NoSuchMethodException exception) {
                LOGGER.error("Registrar {} must expose a public no-arg constructor", className, exception);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException exception) {
                LOGGER.error("Failed to instantiate registrar: {}", className, exception);
            }
        }
        return registrars;
    }

    private static Supplier<Collection<String>> classNamesFor(Class<? extends Annotation> annotationClass) {
        Objects.requireNonNull(annotationClass, "annotationClass");
        return () -> {
            List<String> classNames = new ArrayList<>();
            ModList modList;
            try {
                modList = ModList.get();
            } catch (NoClassDefFoundError | ExceptionInInitializerError exception) {
                LOGGER.debug("NeoForge ModList is unavailable; registrar scan is skipped", exception);
                return classNames;
            }
            if (modList == null) {
                return classNames;
            }
            for (ModFileScanData scanData : modList.getAllScanData()) {
                scanData.getAnnotatedBy(annotationClass, ElementType.TYPE)
                        .forEach(annotationData -> classNames.add(annotationData.memberName()));
            }
            return classNames;
        };
    }
}
