package org.alex73.android;

public class StyledIdString extends StyledString {
    public String id;

    public StyledIdString() {
    }

    public StyledIdString(String id, StyledString str) {
        super(str);
        this.id = id;
    }

    public boolean equals(Object o) {
        boolean r = super.equals(o);
        if (r) {
            StyledIdString other = (StyledIdString) o;
            r = id.equals(other.id);
        }
        return r;
    }

    @Override
    public String toString() {
        return id + "/" + super.toString();
    }
}
