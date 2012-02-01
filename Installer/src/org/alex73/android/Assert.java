package org.alex73.android;

public class Assert {
    public static void assertTrue(String message, boolean value) {
        if (!value) {
            fail(message);
        }
    }

    public static void assertNull(String message, Object value) {
        if (value != null) {
            fail(message);
        }
    }

    public static void assertNotNull(String message, Object value) {
        if (value == null) {
            fail(message);
        }
    }

    public static void assertEquals(String message, int v1, int v2) {
        if (v1 != v2) {
            fail(message);
        }
    }

    public static void fail(String message) {
        throw new RuntimeException(message);
    }
}
