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

        boolean linkToOtherString = false;
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
                        tagStart.tagName += ";" + aName + "=" + rd.getAttributeValue(i);
                    } else {
                        break;
                    }
                }
                tags.add(tagStart);
                tagsStack.push(tagStart);
                break;
            case XMLEvent.END_ELEMENT:
                if (tagsStack.isEmpty()) {
                    StyledString result = new StyledString();
                    result.raw = currentString.toString();
                    result.tags = tags.toArray(new StyledString.Tag[tags.size()]);
                    if (linkToOtherString) {
                        return null;
                    } else {
                        return postProcessString(result);
                    }
                }
                StyledString.Tag tagEnd = tagsStack.pop();
                tagEnd.end = currentString.length() - 1;
                break;
            case XMLEvent.CHARACTERS:
                String text = rd.getText();
                if (currentString.length() == 0 && text.startsWith("@")) {
                    linkToOtherString = true;
                }
                text = postProcessPartString(text);
                currentString.append(text);
                break;
            case XMLEvent.COMMENT:
                break;
            default:
                Assert.fail("Wrong XML event");
                break;
            }
        }
    }

    public static String postProcessPartString(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            char cNext;
            try {
                cNext = str.charAt(i + 1);
            } catch (StringIndexOutOfBoundsException ex) {
                cNext = 0;
            }
            switch (c) {
            case '\\':
                switch (cNext) {
                case '"':
                case '\'':
                case '\\':
                case ' ':
                case '@':
                case '?':
                case '’':
                    str = str.substring(0, i) + cNext + str.substring(i + 2);
                    break;
                case 'r':
                    str = str.substring(0, i) + '\r' + str.substring(i + 2);
                    break;
                case 'n':
                    str = str.substring(0, i) + '\n' + str.substring(i + 2);
                    break;
                case '\n':// hack for ics
                    str = str.substring(0, i) + '\n' + str.substring(i + 2);
                    break;
                case 't':
                    str = str.substring(0, i) + '\t' + str.substring(i + 2);
                    break;
                case 'u':
                    String num = str.substring(i + 2, i + 6);
                    str = str.substring(0, i) + ((char) Integer.parseInt(num, 16)) + str.substring(i + 6);
                    break;
                default:
                    Assert.fail("Unknown quoted char: \\" + cNext);
                }
                break;
            }
        }

        return str;
    }

    public static StyledString postProcessString(StyledString str) {
        str.sortTags();
        return str;
    }
}
