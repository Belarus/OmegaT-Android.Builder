package org.alex73.android.arsc2;

public class LightString implements CharSequence, Comparable<LightString> {
    char base[];
    int offset;
    int length;

    public LightString(String str) {
        this.base = str.toCharArray();
        this.offset = 0;
        this.length = base.length;
    }

    public LightString(char[] base, int off, int len) {
        this.base = base;
        this.offset = off;
        this.length = len;
    }

    public int length() {
        return length;
    }

    public CharSequence subSequence(int start, int end) {
        return new LightString(base, start, end - start);
    }

    public LightString substring(int start, int end) {
        return new LightString(base, start, end - start);
    }

    public char charAt(int index) {
        if ((index < 0) || (index >= length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return base[offset + index];
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public boolean startsWith(String prefix) {
        if (prefix.length() > length) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (base[offset + i] != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    int hash;

    public int hashCode() {
        int h = hash;
        if (h == 0 && length > 0) {
            for (int i = 0; i < length; i++) {
                h = 31 * h + base[offset + i];
            }
            hash = h;
        }
        return h;
    }

    public int indexOf(int ch, int fromIndex) {
        for (int i = fromIndex; i < length; i++) {
            if (base[offset + i] == ch) {
                return i;
            }
        }
        return -1;
    }

    public int compareTo(LightString another) {
        int len1 = length;
        int len2 = another.length;
        int lim = Math.min(len1, len2);
        char v1[] = base;
        char v2[] = another.base;
        int o1 = offset;
        int o2 = another.offset;

        int k = 0;
        while (k < lim) {
            char c1 = v1[o1 + k];
            char c2 = v2[o2 + k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LightString)) {
            return false;
        }
        return compareTo((LightString) o) == 0;
    }

    public String toString() {
        return new String(base, offset, length);
    }
}
