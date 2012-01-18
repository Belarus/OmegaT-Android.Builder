package org.alex73.android.arsc;

import org.junit.Assert;

import junit.framework.TestCase;

public class StringTableTest extends TestCase {
    public void testVar() {
        for (int i = 0; i < 32768; i++) {
            byte[] c = StringTable.constructVarint(i);
            int[] cc = StringTable.getVarint(c, 0);
            Assert.assertEquals(i, cc[0]);
        }
    }
}
