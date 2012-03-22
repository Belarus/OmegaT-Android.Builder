package org.alex73.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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

public class StAXDecoderReader {
    protected XMLInputFactory factory;

    private Map<String, StyledString> strings = new HashMap<String, StyledString>();
    private Map<String, List<StyledString>> arrays = new HashMap<String, List<StyledString>>();
    private Map<String, Map<String, StyledString>> plurals = new HashMap<String, Map<String, StyledString>>();

    private StringBuilder currentString = new StringBuilder();

    public StAXDecoderReader() {
        factory = XMLInputFactory.newFactory();
    }

    public Map<String, StyledString> getStrings() {
        return strings;
    }

    public Map<String, List<StyledString>> getArrays() {
        return arrays;
    }

    public Map<String, Map<String, StyledString>> getPlurals() {
        return plurals;
    }

    public void read(File inFile) throws Exception {
        XMLEventReader rd = factory
                .createXMLEventReader(new BufferedInputStream(new FileInputStream(inFile)));

        String name = null;
        StyledString str = null;
        List<StyledString> array = null;
        Map<String, StyledString> plural = null;
        while (rd.hasNext()) {
            XMLEvent e = rd.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                StartElement eStart = (StartElement) e;
                switch (e.asStartElement().getName().getLocalPart()) {
                case "string":
                    name = eStart.getAttributeByName(new QName("name")).getValue();
                    Attribute product = eStart.getAttributeByName(new QName("product"));
                    if (product != null) {
                        name += '#' + product.getValue();
                    }
                    str = read(rd);
                    Assert.assertNull("", strings.put(name, str));
                    break;
                case "string-array":
                    name = eStart.getAttributeByName(new QName("name")).getValue();
                    array = new ArrayList<StyledString>();
                    Assert.assertNull("", arrays.put(name, array));
                    break;
                case "plurals":
                    name = eStart.getAttributeByName(new QName("name")).getValue();
                    plural = new TreeMap<String, StyledString>();
                    Assert.assertNull("", plurals.put(name, plural));
                    break;
                case "item":
                    Attribute quantity = eStart.getAttributeByName(new QName("quantity"));
                    str = read(rd);
                    if (quantity != null) {
                        Assert.assertNull("", plural.put(quantity.getValue(), str));
                    } else {
                        array.add(str);
                    }
                    break;
                case "resources":
                case "skip":
                    break;
                default:
                    Assert.fail("Wrong element: " + e);
                }
                break;
            }
        }
        rd.close();
    }

    protected StyledString read(XMLEventReader rd) throws Exception {
        List<StyledString.Tag> tags = new ArrayList<StyledString.Tag>();
        Stack<StyledString.Tag> tagsStack = new Stack<StyledString.Tag>();

        currentString.setLength(0);
        while (true) {
            XMLEvent e = rd.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                StartElement eStart = (StartElement) e;
                StyledString.Tag tagStart = new StyledString.Tag();
                tagStart.start = currentString.length();
                tagStart.tagName = eStart.getName().getLocalPart();
                for (Iterator<Attribute> it = eStart.getAttributes(); it.hasNext();) {
                    Attribute a = it.next();
                    tagStart.tagName += ";" + a.getName() + "=" + a.getValue();
                }
                tags.add(tagStart);
                tagsStack.push(tagStart);
                break;
            case XMLEvent.END_ELEMENT:
                EndElement eEnd = (EndElement) e;
                if (tagsStack.isEmpty()) {
                    StyledString result = new StyledString();
                    result.raw = currentString.toString();
                    result.tags = tags.toArray(new StyledString.Tag[tags.size()]);
                    return postProcessString(result);
                }
                StyledString.Tag tagEnd = tagsStack.pop();
                tagEnd.end = currentString.length() - 1;
                break;
            case XMLEvent.CHARACTERS:
                Characters eChar = (Characters) e;
                currentString.append(eChar.getData());
                break;
            case XMLEvent.SPACE:
                Characters eSpace = (Characters) e;
                currentString.append(eSpace.getData());
                break;
            }
        }
    }

    boolean inQuotes;

    public static StyledString postProcessString(StyledString str) {
        str.sortTags();

        // trim
        while (str.raw.length() > 0) {
            if (str.raw.charAt(0) <= ' ') {
                decreaseTagsPos(str, 0, 1);
                str.raw = str.raw.substring(1);
            } else {
                break;
            }
        }
        while (str.raw.length() > 0) {
            if (str.raw.charAt(str.raw.length() - 1) <= ' ') {
                decreaseTagsPos(str, str.raw.length() - 1, 1);
                str.raw = str.raw.substring(0, str.raw.length() - 1);
            } else {
                break;
            }
        }
        // link to other string ?
        if (str.raw.startsWith("@")) {
            return null;
        }
        // remove double spaces
        for (int i = 0; i < str.raw.length(); i++) {
            if (str.raw.charAt(i) <= ' ') {
                str.raw = str.raw.substring(0, i) + ' ' + str.raw.substring(i + 1);
                if (i == 0 || str.raw.charAt(i - 1) <= ' ') {
                    decreaseTagsPos(str, i, 1);
                    str.raw = str.raw.substring(0, i) + str.raw.substring(i + 1);
                    i--;
                }
            }
        }
        // remove quotes
        boolean inQuotes = false;
        for (int i = 0; i < str.raw.length(); i++) {
            char c = str.raw.charAt(i);
            char cNext;
            try {
                cNext = str.raw.charAt(i + 1);
            } catch (StringIndexOutOfBoundsException ex) {
                cNext = 0;
            }
            switch (c) {
            case '"':
                inQuotes = !inQuotes;
                decreaseTagsPos(str, i, 1);
                str.raw = str.raw.substring(0, i) + str.raw.substring(i + 1);
                break;
            case '\\':
                switch (cNext) {
                case '"':
                case '\'':
                case '’':
                    decreaseTagsPos(str, i, 1);
                    str.raw = str.raw.substring(0, i) + cNext + str.raw.substring(i + 2);
                    break;
                case 'r':
                    decreaseTagsPos(str, i, 1);
                    str.raw = str.raw.substring(0, i) + '\r' + str.raw.substring(i + 2);
                    break;
                case 'n':
                    decreaseTagsPos(str, i, 1);
                    str.raw = str.raw.substring(0, i) + '\n' + str.raw.substring(i + 2);
                    break;
                case 't':
                    decreaseTagsPos(str, i, 1);
                    str.raw = str.raw.substring(0, i) + '\t' + str.raw.substring(i + 2);
                    break;
                case 'u':
                    String num = str.raw.substring(i + 2, i + 6);
                    decreaseTagsPos(str, i, 5);
                    str.raw = str.raw.substring(0, i) + ((char) Integer.parseInt(num, 16))
                            + str.raw.substring(i + 6);
                    break;
                default:
                    Assert.fail("Unknown quoted char: \\" + cNext);
                }
                break;
            }
        }
        return str;
    }

    static void decreaseTagsPos(StyledString str, int pos, int num) {
        for (StyledString.Tag tag : str.tags) {
            if (tag.start > pos) {
                tag.start -= num;
            }
            if (tag.end > pos) {
                tag.end -= num;
            }
        }
    }
}
