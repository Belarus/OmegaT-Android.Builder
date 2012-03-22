package org.alex73.android;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

public class StyledString {
    public final static Tag[] NO_TAGS = new Tag[0];
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

    public boolean hasTags() {
        return tags.length > 0;
    }

    public boolean equalsTagNames(StyledString o) {
        if (tags.length != o.tags.length) {
            return false;
        }
        for (int i = 0; i < tags.length; i++) {
            if (!tags[i].tagName.equals(o.tags[i].tagName)) {
                return false;
            }
        }
        return true;
    }

    public void sortTags() {
        Arrays.sort(tags, TAGS_COMPARATOR);
    }

    public int hashCode() {
        return raw.hashCode();
    }

    public boolean equals(Object o) {
        StyledString other = (StyledString) o;
        boolean eq = raw.equals(other.raw);
        if (eq && tags.length != other.tags.length) {
            eq = false;
        }
        return eq;
    }

    @Override
    public String toString() {
        return raw + "/tagsCount=" + tags.length;
    }

    public void dump(PrintStream wr) throws IOException {
        wr.println("text: " + raw );
        wr.println("hash: " + raw.hashCode());
        for (StyledString.Tag tag : tags) {
            wr.println("  tag: " + tag.tagName + "  " + tag.start + '-' + tag.end );
        }
    }

    static Comparator<Tag> TAGS_COMPARATOR = new Comparator<Tag>() {
        public int compare(Tag lhs, Tag rhs) {
            int c = lhs.tagName.compareTo(rhs.tagName);
            if (c == 0) {
                c = lhs.start - rhs.start;
            }
            if (c == 0) {
                c = lhs.end - rhs.end;
            }
            return c;
        }
    };
}
