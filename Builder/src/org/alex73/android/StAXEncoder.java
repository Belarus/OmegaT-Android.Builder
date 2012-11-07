package org.alex73.android;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.alex73.android.StyledString.Tag;
import org.alex73.android.arsc.Config;
import org.alex73.android.arsc.Entry;
import org.alex73.android.arsc.Resources;
import org.alex73.android.arsc2.LightString;
import org.apache.commons.io.FileUtils;

import android.schema.ResXmlStringArray;
import android.util.TypedValue;
import brut.androlib.res.data.value.ResArrayValue;
import brut.androlib.res.data.value.ResPluralsValue;

public class StAXEncoder implements IEncoder {
    private XMLOutputFactory factory;

    public StAXEncoder() {
        factory = XMLOutputFactory.newFactory();
    }

    public void dump(Resources rs, File outDir, String versionSuffix) throws Exception {
        for (org.alex73.android.arsc.Package pkg : rs.getPackages()) {
            String suffix = "";
            if (rs.getPackages().length > 1 && !"android".equals(pkg.getName())) {
                suffix = "-" + pkg.getName();
            } else {
                suffix = "";
            }
            suffix += '_' + versionSuffix;

            String s = dumpStrings(rs, pkg, outDir, suffix);
            save(new File(outDir, "strings" + suffix + ".xml"), s);
            String a = dumpArrays(rs, pkg, outDir, suffix);
            save(new File(outDir, "arrays" + suffix + ".xml"), a);
            String p = dumpPlurals(rs, pkg, outDir, suffix);
            save(new File(outDir, "plurals" + suffix + ".xml"), p);
        }
    }

    protected void save(File outFile, String data) throws Exception {
        if (data != null) {
            FileUtils.writeStringToFile(outFile, data, "UTF-8");
        }
    }

    protected String dumpStrings(Resources rs, org.alex73.android.arsc.Package pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config c : pkg.getAllConfigs()) {
            if ("string".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (Entry e : c.getEntries()) {
                        if (e instanceof Entry.SimpleEntry) {
                            StyledString str = ((Entry.SimpleEntry) e).styledStringValue;
                            if (str != null && !str.hasInvalidChars()) {
                                hasData = true;
                                wr.writeCharacters("    ");
                                wr.writeStartElement("string");
                                wr.writeAttribute("name", e.getName());
                                write(wr, str);
                                wr.writeEndElement();
                                wr.writeCharacters("\n");
                            }
                        }
                    }
                }
            }
        }
        wr.writeEndElement();
        // wr.writeCharacters("\n");
        wr.writeEndDocument();

        return hasData ? out.toString() : null;
    }

    protected String dumpArrays(Resources rs, org.alex73.android.arsc.Package pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config c : pkg.getAllConfigs()) {
            if ("array".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (Entry e : c.getEntries()) {
                        if (e instanceof Entry.ComplexEntry) {
                            Entry.ComplexEntry ec = (Entry.ComplexEntry) e;
                            if (ec.values.length > 0 && isStringArray(ec)) {
                                hasData = true;
                                wr.writeCharacters("    ");
                                wr.writeStartElement("string-array");
                                wr.writeAttribute("name", e.getName());
                                wr.writeCharacters("\n");
                                ResXmlStringArray sa = new ResXmlStringArray();
                                sa.setName(e.getName());
                                for (Entry.KeyValue kv : ec.values) {
                                    StyledString str = rs.getStringTable().getStyledString(kv.vData);
                                    if (!str.hasInvalidChars()) {
                                        wr.writeCharacters("        ");
                                        wr.writeStartElement("item");
                                        write(wr, str);
                                        wr.writeEndElement();
                                        wr.writeCharacters("\n");
                                    }
                                }
                                wr.writeCharacters("    ");
                                wr.writeEndElement();
                                wr.writeCharacters("\n");
                            }
                        }
                    }
                }
            }
        }
        wr.writeEndElement();
        // wr.writeCharacters("\n");
        wr.writeEndDocument();

        return hasData ? out.toString() : null;
    }

    protected String dumpPlurals(Resources rs, org.alex73.android.arsc.Package pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config c : pkg.getAllConfigs()) {
            if ("plurals".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (Entry e : c.getEntries()) {
                        if (e instanceof Entry.ComplexEntry) {
                            Entry.ComplexEntry ec = (Entry.ComplexEntry) e;
                            Assert.assertTrue("", isPluralsArray(ec));
                            hasData = true;
                            wr.writeCharacters("    ");
                            wr.writeStartElement("plurals");
                            wr.writeAttribute("name", e.getName());
                            wr.writeCharacters("\n");
                            ResXmlStringArray sa = new ResXmlStringArray();
                            sa.setName(e.getName());
                            for (Entry.KeyValue kv : ec.values) {
                                StyledString str = rs.getStringTable().getStyledString(kv.vData);
                                if (!str.hasInvalidChars()) {
                                    wr.writeCharacters("        ");
                                    wr.writeStartElement("item");
                                    wr.writeAttribute("quantity", ARSC.QUANTITY_MAP[kv.key
                                            - ARSC.BAG_KEY_PLURALS_START]);
                                    write(wr, str);
                                    wr.writeEndElement();
                                    wr.writeCharacters("\n");
                                }
                            }
                            wr.writeCharacters("    ");
                            wr.writeEndElement();
                            wr.writeCharacters("\n");
                        }
                    }
                }
            }
        }
        wr.writeEndElement();
        // wr.writeCharacters("\n");
        wr.writeEndDocument();

        return hasData ? out.toString() : null;
    }

    @Override
    public String marshall(StyledString str) throws Exception {
        StringWriter o = new StringWriter(2000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(o);

        write(wr, str);

        String r = o.toString();
        if (r.startsWith("@")) {
            r = '\\' + r;
        }
        return r;
    }

    protected void write(XMLStreamWriter wr, StyledString str) throws Exception {
        Arrays.sort(str.tags, TAGS_COMPARATOR);

        for (int i = 0; i < str.raw.length(); i++) {
            for (int j = 0; j < str.tags.length; j++) {
                if (str.tags[j].start == i) {
                    String[] ta = str.tags[j].tagName.toString().split(";");
                    wr.writeStartElement(ta[0]);
                    for (int k = 1; k < ta.length; k++) {
                        int pos = ta[k].indexOf('=');
                        Assert.assertTrue("", pos > 0);
                        wr.writeAttribute(ta[k].substring(0, pos), ta[k].substring(pos + 1));
                    }
                }
            }
            writeText(wr, str.raw.substring(i, i + 1));
            for (int j = 0; j < str.tags.length; j++) {
                if (str.tags[j].end == i) {
                    wr.writeEndElement();
                }
            }
        }
    }

    StringBuilder writeTextBuffer = new StringBuilder();

    protected void writeText(XMLStreamWriter wr, LightString text) throws Exception {
        writeTextBuffer.setLength(0);
        for (int i=0;i<text.length();i++) {
            char c = text.charAt(i);
            switch (c) {
            case '\n':
                writeTextBuffer.append("\\n");
                break;
            case '\r':
                writeTextBuffer.append("\\r");
                break;
            case '\t':
                writeTextBuffer.append("\\t");
                break;
            case '\\':
            case '\'':
            case '"':
                writeTextBuffer.append('\\');
                writeTextBuffer.append(c);
                break;
            default:
                writeTextBuffer.append(c);
                break;
            }
        }
        wr.writeCharacters(writeTextBuffer.toString());
    }

    protected static boolean isStringArray(Entry.ComplexEntry kvs) {
        Assert.assertEquals("", ResArrayValue.BAG_KEY_ARRAY_START, kvs.values[0].key);
        boolean hasStrings = false;
        boolean hasOther = false;
        for (Entry.KeyValue kv : kvs.values) {
            if (kv.vType == TypedValue.TYPE_STRING) {
                hasStrings = true;
            } else {
                hasOther = true;
            }
        }
        return hasStrings && !hasOther;
    }

    protected static boolean isPluralsArray(Entry.ComplexEntry kvs) {
        Assert.assertTrue("", kvs.values.length <= ARSC.QUANTITY_MAP.length);
        for (Entry.KeyValue kv : kvs.values) {
            Assert.assertTrue("", kv.key >= ResPluralsValue.BAG_KEY_PLURALS_START
                    && kv.key <= ResPluralsValue.BAG_KEY_PLURALS_END);
        }
        return true;
    }

    Comparator<StyledString.Tag> TAGS_COMPARATOR = new Comparator<StyledString.Tag>() {
        public int compare(Tag o1, Tag o2) {
            // спачатку тыя што бліжэй да пачатку радка
            int d = o1.start - o2.start;
            if (d == 0) {
                // потым спачатку большыя па памеру
                int sz1 = o1.end - o1.start;
                int sz2 = o2.end - o2.start;
                d = sz2 - sz1;
            }
            if (d == 0) {
                // потым па альфабэце
                d = o1.tagName.compareTo(o2.tagName);
            }
            return d;
        }
    };
}
