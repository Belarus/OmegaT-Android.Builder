package org.alex73.android.arsc;

import org.alex73.android.StyledString;

public abstract class Entry {
    protected int id;

    protected String name;

    protected short flags;

    public String getName() {
        return name;
    }

    public static class KeyValue {
        public int key;
        public Object value;
        public byte vType;
        public int vData;
    }

    public static class SimpleEntry extends Entry {
        public byte vType;
        public int vData;
        public StyledString styledStringValue;
    }

    public static class ComplexEntry extends Entry {
        public int vParent;
        public KeyValue[] values;
    }
}
