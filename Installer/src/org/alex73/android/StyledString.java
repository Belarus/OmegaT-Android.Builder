package org.alex73.android;

public class StyledString {
    public String raw;
    public Tag[] tags;

    public static class Tag {
        public String tagName;
        public int start;
        public int end;

        @Override
        public boolean equals(Object obj) {
            Tag t = (Tag) obj;
            return start == t.start && end == t.end && tagName.equals(t.tagName);
        }

        @Override
        public String toString() {
            return tagName + "(" + start + ":" + end + ")";
        }
    }

    public boolean hasInvalidChars() {
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 0x20) {
                if (c != 0x09 && c != 0x0A && c != 0x0D) {
                    return true;
                }
            } else if (c >= 0x20 && c <= 0xD7FF) {
            } else if (c >= 0xE000 && c <= 0xFFFD) {
            } else if (c >= 0x10000 && c <= 0x10FFFF) {
            } else {
                return true;
            }
        }
        return false;
    }
}
