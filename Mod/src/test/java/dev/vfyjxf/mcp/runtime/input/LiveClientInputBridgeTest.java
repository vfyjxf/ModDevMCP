package dev.vfyjxf.mcp.runtime.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveClientInputBridgeTest {

    @Test
    void readBooleanFieldFindsInheritedFieldOnSubclassedWidgets() throws Exception {
        class EditableBase {
            @SuppressWarnings("unused")
            private final boolean isEditable = true;
        }
        class EditableSubclass extends EditableBase {
        }

        assertEquals(true, ReflectiveFieldAccess.readBooleanField(new EditableSubclass(), "isEditable"));
    }
}
