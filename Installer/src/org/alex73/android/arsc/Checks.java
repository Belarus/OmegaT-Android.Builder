package org.alex73.android.arsc;

public class Checks {
    public static void expect(int required, int value) {
        if (required != value) {
            throw new RuntimeException();
        }
    }

    public static void expect(int required, int value, String error) {
        if (required != value) {
            throw new RuntimeException(error);
        }
    }
}
