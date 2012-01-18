package org.alex73.android;
import javax.xml.bind.JAXBContext;

import android.schema.ResXmlResources;

public class ARSC {
    public static final int BAG_KEY_PLURALS_START = 0x01000004;
    public static final String[] QUANTITY_MAP = new String[] { "other", "zero", "one", "two", "few", "many" };

    public static final JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(ResXmlResources.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
