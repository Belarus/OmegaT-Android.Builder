package org.alex73.android;

public class StyledIdString extends StyledString {
    public CharSequence id;

    public boolean equals(Object o) {
        boolean r = super.equals(o);
        if (r) {
            StyledIdString other = (StyledIdString) o;
            r = id.equals(other.id);
        }
        return r;
    }
}
