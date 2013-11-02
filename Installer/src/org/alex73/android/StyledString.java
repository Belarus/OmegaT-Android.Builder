package org.alex73.android;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    public StyledString() {
    }

    public StyledString(StyledString str) {
        this.raw = str.raw;
        this.tags = str.tags;
    }

    public boolean isEmpty() {
        return raw.length() == 0 && tags.length == 0;
    }

    public boolean hasTags() {
        return tags.length > 0;
    }

    public boolean equalsTagNames(StyledString o) {
        if (tags.length != o.tags.length) {
            return false;
        }
        List<String> tagNames = new ArrayList<String>();
        for (int i = 0; i < tags.length; i++) {
            tagNames.add(tags[i].tagName);
        }
        for (int i = 0; i < o.tags.length; i++) {
            tagNames.remove(o.tags[i].tagName);
        }
        return tagNames.isEmpty();
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

    public void decreaseTagsPos(int pos, int num) {
        for (StyledString.Tag tag : tags) {
            if (tag.start >= pos) {
                tag.start -= num;
            }
            if (tag.end >= pos) {
                tag.end -= num;
            }
        }
    }

    public void removeSpaces() {
        if (true)
            return;
        boolean wasSpace = true;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) <= ' ') {
                if (wasSpace) {
                    raw = raw.substring(0, i) + "" + raw.substring(i + 1, raw.length());
                    decreaseTagsPos(i, 1);
                    i--;
                } else {
                    raw = raw.substring(0, i) + " " + raw.substring(i + 1, raw.length());
                    wasSpace = true;
                }
            } else {
                wasSpace = false;
            }
        }
        int se = raw.length() - 1;
        if (se >= 0 && raw.charAt(se) <= ' ') {
            raw = raw.substring(0, se);
            decreaseTagsPos(se, 1);
        }
    }

    @Override
    public String toString() {
        return raw + "/tagsCount=" + tags.length;
    }

    public void dump(PrintStream wr) throws IOException {
        wr.println("text: " + raw);
        wr.println("hash: " + raw.hashCode());
        for (StyledString.Tag tag : tags) {
            wr.println("  tag: " + tag.tagName + "  " + tag.start + '-' + tag.end);
        }
    }

    static Comparator<Tag> TAGS_COMPARATOR = new Comparator<Tag>() {
        public int compare(Tag lhs, Tag rhs) {
            int c = 0;
            if (c == 0) {
                c = lhs.start - rhs.start;
            }
            if (c == 0) {
                c = rhs.end - lhs.end;
            }
            if (c == 0) {
                c = lhs.tagName.compareTo(rhs.tagName);
            }
            return c;
        }
    };
}
