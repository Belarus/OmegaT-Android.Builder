package org.alex73.android;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.alex73.android.StyledString.Tag;
import org.alex73.android.arsc2.Config2;
import org.alex73.android.arsc2.Entry2;
import org.alex73.android.arsc2.Package2;
import org.alex73.android.arsc2.ResourceProcessor;
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

    public void dump(ResourceProcessor rs, File outDir, String versionSuffix) throws Exception {
        for (Package2 pkg : rs.packages) {
            String suffix = "";
            if (rs.packages.length > 1 && !"android".equals(pkg.getName())) {
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

    protected String dumpStrings(ResourceProcessor rs, Package2 pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config2 c : pkg.getAllConfigs()) {
            if ("string".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (int i=0;i<c.getEntriesCount();i++) {
                        Entry2 e = c.getEntry(i);
                        if (e!=null && !e.isComplex()) {
                            int idx = e.getSimpleStringIndex();
                            if (idx >= 0) {
                                StyledString str = rs.globalStringTable.getStrings().get(idx).getStyledString();
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

    protected String dumpArrays(ResourceProcessor rs, Package2 pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config2 c : pkg.getAllConfigs()) {
            if ("array".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (int i=0;i<c.getEntriesCount();i++) {
                        Entry2 e = c.getEntry(i);
                        if (e!=null && e.isComplex()) {
                            if (e.getKeyValues().length > 0 && isStringArray(e.getKeyValues())) {
                                hasData = true;
                                wr.writeCharacters("    ");
                                wr.writeStartElement("string-array");
                                wr.writeAttribute("name", e.getName());
                                wr.writeCharacters("\n");
                                ResXmlStringArray sa = new ResXmlStringArray();
                                sa.setName(e.getName());
                                for (Entry2.KeyValue kv : e.getKeyValues()) {
                                    StyledString str = rs.globalStringTable.getStrings().get(kv.complexData).getStyledString();
                                    wr.writeCharacters("        ");
                                    wr.writeStartElement("item");
                                    write(wr, str);
                                    wr.writeEndElement();
                                    wr.writeCharacters("\n");
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

    protected String dumpPlurals(ResourceProcessor rs, Package2 pkg, File outDir, String suffix)
            throws Exception {
        StringWriter out = new StringWriter(100000);
        XMLStreamWriter wr = factory.createXMLStreamWriter(out);
        boolean hasData = false;

        wr.writeStartDocument("UTF-8", "1.0");
        wr.writeCharacters("\n");
        wr.writeStartElement("resources");
        wr.writeCharacters("\n");

        for (Config2 c : pkg.getAllConfigs()) {
            if ("plurals".equals(c.getParentType().getName())) {
                if (c.getFlags().isEmpty()) {
                    // base translation
                    for (int i=0;i<c.getEntriesCount();i++) {
                        Entry2 e = c.getEntry(i);
                        if (e!=null && e.isComplex()) {
                            Assert.assertTrue("", isPluralsArray(e.getKeyValues()));
                            hasData = true;
                            wr.writeCharacters("    ");
                            wr.writeStartElement("plurals");
                            wr.writeAttribute("name", e.getName());
                            wr.writeCharacters("\n");
                            ResXmlStringArray sa = new ResXmlStringArray();
                            sa.setName(e.getName());
                            for (Entry2.KeyValue kv : e.getKeyValues()) {
                                StyledString str = rs.globalStringTable.getStrings().get(kv.complexData).getStyledString();
                                wr.writeCharacters("        ");
                                wr.writeStartElement("item");
                                wr.writeAttribute("quantity", ARSC.QUANTITY_MAP[kv.key - ARSC.BAG_KEY_PLURALS_START]);
                                write(wr, str);
                                wr.writeEndElement();
                                wr.writeCharacters("\n");
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
                    String[] ta = str.tags[j].tagName.split(";");
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

    protected void writeText(XMLStreamWriter wr, String text) throws Exception {
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

    protected static boolean isStringArray(Entry2.KeyValue[] kvs) {
        Assert.assertEquals("", ResArrayValue.BAG_KEY_ARRAY_START, kvs[0].key);
        boolean hasStrings = false;
        boolean hasOther = false;
        for (Entry2.KeyValue kv : kvs) {
            if (kv.complexType == TypedValue.TYPE_STRING) {
                hasStrings = true;
            } else {
                hasOther = true;
            }
        }
        return hasStrings && !hasOther;
    }

    protected static boolean isPluralsArray(Entry2.KeyValue[] kvs) {
        Assert.assertTrue("", kvs.length <= ARSC.QUANTITY_MAP.length);
        for (Entry2.KeyValue kv : kvs) {
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
