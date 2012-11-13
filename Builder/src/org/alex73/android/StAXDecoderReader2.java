package org.alex73.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class StAXDecoderReader2 {
    protected XMLInputFactory factory;

    private Map<String, StyledString> strings = new HashMap<String, StyledString>();
    private Map<String, List<StyledString>> arrays = new HashMap<String, List<StyledString>>();
    private Map<String, Map<String, StyledString>> plurals = new HashMap<String, Map<String, StyledString>>();

    private StringBuilder currentString = new StringBuilder();

    public StAXDecoderReader2() {
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
        XMLStreamReader rd = factory.createXMLStreamReader(new BufferedInputStream(new FileInputStream(inFile)));

        String name = null;
        StyledString str = null;
        List<StyledString> array = null;
        Map<String, StyledString> plural = null;
        while (rd.hasNext()) {
            switch (rd.next()) {
            case XMLEvent.START_ELEMENT:
                Map<String, String> attrs = readAttributes(rd);
                switch (rd.getLocalName()) {
                case "string":
                    name = attrs.get("name");
                    String product = attrs.get("product");
                    if (product != null) {
                        name += '#' + product;
                    }
                    str = read(rd);
                    Assert.assertNull("", strings.put(name, str));
                    break;
                case "string-array":
                    name = attrs.get("name");
                    array = new ArrayList<StyledString>();
                    Assert.assertNull("", arrays.put(name, array));
                    break;
                case "plurals":
                    name = attrs.get("name");
                    plural = new TreeMap<String, StyledString>();
                    Assert.assertNull("", plurals.put(name, plural));
                    break;
                case "item":
                    String quantity = attrs.get("quantity");
                    str = read(rd);
                    if (plural != null && quantity != null) {
                        Assert.assertNull("", plural.put(quantity, str));
                    } else if (array != null) {
                        array.add(str);
                    }
                    break;
                case "resources":
                case "skip":
                case "integer-array":
                case "array":
                case "color":
                case "add-resource":
                case "eat-comment":
                    break;
                default:
                    Assert.fail("Wrong XML element: " + rd.getLocalName());
                }
                break;
            }
        }
        rd.close();
    }

    protected Map<String, String> readAttributes(XMLStreamReader rd) {
        Map<String, String> result = new TreeMap<String, String>();
        for (int i = 0;; i++) {
            String aName = rd.getAttributeLocalName(i);
            if (aName != null) {
                result.put(aName, rd.getAttributeValue(i));
            } else {
                break;
            }
        }
        return result;
    }

    protected StyledString read(XMLStreamReader rd) throws Exception {
        List<StyledString.Tag> tags = new ArrayList<StyledString.Tag>();
        Stack<StyledString.Tag> tagsStack = new Stack<StyledString.Tag>();

        currentString.setLength(0);
        while (true) {
            switch (rd.next()) {
            case XMLEvent.START_ELEMENT:
                StyledString.Tag tagStart = new StyledString.Tag();
                tagStart.start = currentString.length();
                tagStart.tagName = rd.getLocalName();
                for (int i = 0;; i++) {
                    String aName = rd.getAttributeLocalName(i);
                    if (aName != null) {
                        tagStart.tagName = tagStart.tagName+ ";" + aName + "=" + rd.getAttributeValue(i);
                    } else {
                        break;
                    }
                }
                tags.add(tagStart);
                tagsStack.push(tagStart);
                break;
            case XMLEvent.END_ELEMENT:
                if (tagsStack.isEmpty()) {
                    return postProcessString(currentString, tags);
                }
                StyledString.Tag tagEnd = tagsStack.pop();
                tagEnd.end = currentString.length() - 1;
                break;
            case XMLEvent.CHARACTERS:
                currentString.append(rd.getText());
                break;
            case XMLEvent.COMMENT:
                break;
            default:
                Assert.fail("Wrong XML event");
                break;
            }
        }
    }

    public static StyledString postProcessString(StringBuilder str, List<StyledString.Tag> tags) {
        if (str.length() > 0 && str.charAt(0) == '@') {
            return null;
        }

        StringBuilder out = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
            case '\\':
                int skip = 1;
                char cNext = str.charAt(i + 1);
                switch (cNext) {
                case '"':
                case '\'':
                case '\\':
                case ' ':
                case '@':
                case '?':
                case '’':
                    c = cNext;
                    break;
                case 'r':
                    c = '\r';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case '\n':// hack for ics
                    c = '\n';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'u':
                    String num = str.substring(i + 2, i + 6);
                    c = ((char) Integer.parseInt(num, 16));
                    skip += 4;
                    break;
                default:
                    Assert.fail("Unknown quoted char: '\\" + cNext + "'");
                }
                for (StyledString.Tag t : tags) {
                    if (t.start > out.length()) {
                        t.start -= skip;
                    }
                    if (t.end > out.length()) {
                        t.end -= skip;
                    }
                }
                i += skip;
                break;
            }
            out.append(c);
        }

        StyledString result = new StyledString();
        result.raw = out.toString();
        result.tags = tags.toArray(new StyledString.Tag[tags.size()]);
        result.sortTags();
        return result;
    }
}
