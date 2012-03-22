package org.alex73.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.alex73.android.arsc.ChunkReader;
import org.alex73.android.arsc.Config;
import org.alex73.android.arsc.Entry;
import org.alex73.android.arsc.Resources;

public class StAXDecoder extends StAXDecoderReader implements IDecoder {

    public void reads(Resources rs, File inDir, String versionSuffix) throws Exception {
        for (org.alex73.android.arsc.Package pkg : rs.getPackages()) {
            String suffix = "";
            if (rs.getPackages().length > 1 && !"android".equals(pkg.getName())) {
                suffix = "-" + pkg.getName();
            } else {
                suffix = "";
            }
            suffix += '_' + versionSuffix;

            readStrings(rs, pkg, inDir, suffix);
            readArrays(rs, pkg, inDir, suffix);
            readPlurals(rs, pkg, inDir, suffix);
        }
    }

    protected void readStrings(Resources rs, org.alex73.android.arsc.Package pkg, File inDir, String suffix)
            throws Exception {
        File inFile = new File(inDir, "strings" + suffix + ".xml");
        if (!inFile.exists()) {
            return;
        }

        Map<String, StyledString> strings = readStrings(inFile);

        Config cBel = duplicateBaseConfig(pkg, "string");

        for (Entry e : cBel.getEntries()) {
            if (e instanceof Entry.SimpleEntry) {
                StyledString str = strings.get(e.getName());
                if (str != null) {
                    ((Entry.SimpleEntry) e).vData = rs.getStringTable().addStyledString(str);
                }
            }
        }
    }

    protected void readArrays(Resources rs, org.alex73.android.arsc.Package pkg, File inDir, String suffix)
            throws Exception {
        File inFile = new File(inDir, "arrays" + suffix + ".xml");
        if (!inFile.exists()) {
            return;
        }

        Map<String, List<StyledString>> arrays = readArrays(inFile);

        Config cBel = duplicateBaseConfig(pkg, "array");
        for (Entry e : cBel.getEntries()) {
            if (e instanceof Entry.ComplexEntry) {
                Entry.ComplexEntry ec = (Entry.ComplexEntry) e;
                List<StyledString> str = arrays.get(e.getName());
                if (str != null) {
                    Assert.assertEquals("", ec.values.length, str.size());
                    for (int i = 0; i < ec.values.length; i++) {
                        ec.values[i].vData = rs.getStringTable().addStyledString(str.get(i));
                    }
                }
            }
        }
    }

    protected void readPlurals(Resources rs, org.alex73.android.arsc.Package pkg, File inDir, String suffix)
            throws Exception {
        File inFile = new File(inDir, "plurals" + suffix + ".xml");
        if (!inFile.exists()) {
            return;
        }

        Map<String, Map<String, StyledString>> plurals = readPlurals(inFile);

        Config cBel = duplicateBaseConfig(pkg, "plurals");
        for (Entry e : cBel.getEntries()) {
            if (e instanceof Entry.ComplexEntry) {
                Entry.ComplexEntry ec = (Entry.ComplexEntry) e;
                Map<String, StyledString> str = plurals.get(e.getName());
                if (str != null) {
                    Assert.assertEquals("", ec.values.length, str.size());
                    for (int i = 0; i < ec.values.length; i++) {
                        String q = ARSC.QUANTITY_MAP[ec.values[i].key - ARSC.BAG_KEY_PLURALS_START];
                        ec.values[i].vData = rs.getStringTable().addStyledString(str.get(q));
                    }
                }
            }
        }
    }

    private Config duplicateBaseConfig(org.alex73.android.arsc.Package pkg, String typeName) {
        Config orig = null;
        for (Config c : pkg.getAllConfigs()) {
            if (typeName.equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    Assert.assertNull("", orig);
                    orig = c;
                }
            }
        }
        Assert.assertNotNull("", orig);

        byte[] readed = orig.getOriginalBytes();

        Config cBel = new Config(new ChunkReader(readed));
        int cIndex = pkg.getContent().indexOf(orig);
        pkg.getContent().add(cIndex + 1, cBel);
        cBel.read(pkg);

        cBel.getFlags().setLanguage("be");

        return cBel;
    }

    @Override
    public StyledString unmarshall(String str) throws Exception {
        XMLEventReader rd = factory.createXMLEventReader(new StringReader("<ROOT>" + str + "</ROOT>"));

        XMLEvent e;
        e = rd.nextEvent();
        Assert.assertEquals("", XMLEvent.START_DOCUMENT, e.getEventType());
        e = rd.nextEvent();
        Assert.assertEquals("", XMLEvent.START_ELEMENT, e.getEventType());

        return read(rd);
    }
}
