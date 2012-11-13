package org.alex73.android.arsc2.translation;

public class HashFilter {

    public static Range filterHashes(int[] hash, int h) {
        int minIndex = 0;
        int maxIndex = hash.length - 1;
        int index = 0;
        while (minIndex <= maxIndex) {
            index = (minIndex + maxIndex) >>> 1;
            if (hash[index] < h) {
                minIndex = index + 1;
            } else if (hash[index] > h) {
                maxIndex = index - 1;
            } else {
                break;
            }
        }
        if (hash[index] == h) {
            minIndex = index;
            maxIndex = index;
            while (minIndex > 0 && hash[minIndex - 1] == h) {
                minIndex--;
            }
            while (maxIndex < hash.length - 1 && hash[maxIndex + 1] == h) {
                maxIndex++;
            }
        } else {
            minIndex = 1;
            maxIndex = 0;
        }
        Range r = new Range();
        r.min = minIndex;
        r.max = maxIndex;
        return r;
    }

    public static class Range {
        public int min;
        public int max;
    }
}
