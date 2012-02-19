package org.alex73.android.arsc2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One thread only !!!
 */
public class Segmenter {
    static SegmentationRule[] rules;
    static List<StringBuilder> spaces = new ArrayList<StringBuilder>();
    static StringBuilder glueBuffer = new StringBuilder();
    static List<Integer> rulebreaks = new ArrayList<Integer>();
    static Set<Integer> dontbreakpositions = new TreeSet<Integer>();
    static Set<Integer> breakpositions = new TreeSet<Integer>();

    public static class SegmentationRule {
        public boolean breakRule;
        public Pattern beforeBreak, afterBreak;
    }

    public static List<String> segment(String paragraph) {
        spaces.clear();
        List<String> segments = breakParagraph(paragraph);
        List<String> sentences = new ArrayList<String>(segments.size());

        for (String one : segments) {
            int len = one.length();
            int b = 0;
            StringBuilder bs = new StringBuilder();
            while (b < len && Character.isWhitespace(one.charAt(b))) {
                bs.append(one.charAt(b));
                b++;
            }

            int e = len - 1;
            StringBuilder es = new StringBuilder();
            while (e >= b && Character.isWhitespace(one.charAt(e))) {
                es.append(one.charAt(e));
                e--;
            }
            es.reverse();

            String trimmed = one.substring(b, e + 1);
            sentences.add(trimmed);
            spaces.add(bs);
            spaces.add(es);
        }
        return sentences;
    }

    public static String glue(String[] sentences) {
        if (sentences.length <= 0)
            return "";

        glueBuffer.setLength(0);
        glueBuffer.append(sentences[0]);

        for (int i = 1; i < sentences.length; i++) {
            glueBuffer.append(spaces.get(2 * i - 1));
            glueBuffer.append(spaces.get(2 * i));
            glueBuffer.append(sentences[i]);
        }
        return glueBuffer.toString();
    }

    private static List<String> breakParagraph(String paragraph) {
        dontbreakpositions.clear();
        breakpositions.clear();
        for (int i = rules.length - 1; i >= 0; i--) {
            SegmentationRule rule = rules[i];
            calcBreaks(paragraph, rule);
            if (rule.breakRule) {
                breakpositions.addAll(rulebreaks);
                dontbreakpositions.removeAll(rulebreaks);
            } else {
                dontbreakpositions.addAll(rulebreaks);
                breakpositions.removeAll(rulebreaks);
            }
        }
        breakpositions.removeAll(dontbreakpositions);

        // and now breaking the string according to the positions
        List<String> segments = new ArrayList<String>();
        int prevpos = 0;
        for (int bposition : breakpositions) {
            String oneseg = paragraph.substring(prevpos, bposition);
            segments.add(oneseg);
            prevpos = bposition;
        }
        try {
            String oneseg = paragraph.substring(prevpos);

            // Sometimes the last segment may be empty,
            // it happens for paragraphs like "Rains. "
            // So if it's an empty segment and there's a previous segment
            if (oneseg.trim().length() == 0 && segments.size() > 0) {
                String prev = segments.get(segments.size() - 1);
                prev += oneseg;
                segments.set(segments.size() - 1, prev);
            } else
                segments.add(oneseg);
        } catch (IndexOutOfBoundsException iobe) {
        }

        return segments;
    }

    private static void calcBreaks(String paragraph, SegmentationRule rule) {
        rulebreaks.clear();

        Matcher bbm = rule.beforeBreak.matcher(paragraph);
        Matcher abm = rule.afterBreak.matcher(paragraph);

        if (abm != null)
            if (!abm.find())
                return;

        while (bbm.find()) {
            int bbe = bbm.end();
            int abs = abm.start();
            while (abs < bbe) {
                boolean found = abm.find();
                if (!found)
                    return;
                abs = abm.start();
            }
            if (abs == bbe)
                rulebreaks.add(bbe);
        }
    }
}
