import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.omegat.util.OConsts;

public class TagsFilter {
    static XMLInputFactory factoryIn;
    static XMLOutputFactory factoryOut;

    static {
        factoryIn = XMLInputFactory.newInstance();
        factoryIn.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        factoryIn.setXMLReporter(new XMLReporter() {
            public void report(String message, String error_type, Object info, Location location)
                    throws XMLStreamException {
                System.err.println(message + ": " + info);
            }
        });
        factoryOut = XMLOutputFactory.newInstance();
    }

    XMLEventReader reader;
    XMLStreamWriter writer;

    void filter(String xml, File fileOut, Set<String> nonTraslatable) throws Exception {
        Reader in = new StringReader(xml);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOut));

        reader = factoryIn.createXMLEventReader(in);
        writer = factoryOut.createXMLStreamWriter(out, OConsts.UTF8);

        boolean firstComment = true;

        String name = null;
        String pluralName = null;
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                StartDocument startDocument = (StartDocument) e;
                writer.writeStartDocument("utf-8", startDocument.getVersion());
                writer.writeCharacters("\n");
                break;
            case XMLEvent.END_DOCUMENT:
                writer.writeEndDocument();
                writer.writeCharacters("\n");
                break;
            case XMLEvent.START_ELEMENT:
                StartElement startElement = e.asStartElement();
                switch (startElement.getName().getLocalPart()) {
                case "string":
                    name = getAttribute(startElement, "name");
                    String product = getAttribute(startElement, "product");
                    if (product != null) {
                        name += '#' + product;
                    }
                    if (nonTraslatable.contains(name)) {
                        skipToEnd("string");
                    } else {
                        writeStartElement(startElement);
                        writeToEnd("string");
                    }
                    break;
                case "string-array":
                case "integer-array":
                case "array":
                    name = getAttribute(startElement, "name") + "*array";
                    if (nonTraslatable.contains(name)) {
                        skipToEnd(startElement.getName().getLocalPart());
                    } else {
                        writeStartElement(startElement);
                        writeToEnd(startElement.getName().getLocalPart());
                    }
                    break;
                case "plurals":
                    pluralName = getAttribute(startElement, "name");
                    writeStartElement(startElement);
                    break;
                case "item":
                    String quantity = getAttribute(startElement, "quantity");
                    if (pluralName != null && quantity != null) {
                        name = pluralName + '/' + quantity;
                        if (nonTraslatable.contains(name)) {
                            skipToEnd("item");
                        } else {
                            writeStartElement(startElement);
                            writeToEnd("item");
                        }
                    } else {
                        writeStartElement(startElement);
                    }
                    break;
                case "resources":
                case "add-resource":
                case "eat-comment":
                case "skip":
                case "color":
                    writeStartElement(startElement);
                    break;
                default:
                    throw new RuntimeException("Unknown tag: " + startElement.getName().getLocalPart());
                }
                break;
            case XMLEvent.END_ELEMENT:
                writer.writeEndElement();
                break;
            case XMLEvent.COMMENT:
                Comment comment = (Comment) e;
                String c = comment.getText();
                if (!c.toLowerCase().contains("do not translate")
                        && !c.toLowerCase().contains("don't translate")) {
                    writer.writeComment(c);
                }
                if (firstComment) {
                    writer.writeCharacters("\n");
                    firstComment = false;
                }
                break;
            case XMLEvent.CHARACTERS:
                writer.writeCharacters(e.asCharacters().getData());
                break;
            default:
                throw new RuntimeException();
            }
        }

        reader.close();
        writer.close();
        in.close();
        out.close();
    }

    String getAttribute(StartElement e, String attributeName) {
        Attribute attr = e.getAttributeByName(new QName(attributeName));
        return attr != null ? attr.getValue() : null;
    }

    void writeStartElement(StartElement e) throws Exception {
        writer.writeStartElement(e.getName().getLocalPart());
        for (Iterator<Attribute> it = e.getAttributes(); it.hasNext();) {
            Attribute a = it.next();
            if (a.getName().getLocalPart().equals("translate")
                    || a.getName().getLocalPart().equals("translatable")) {
                continue;
            }
            writer.writeAttribute(a.getName().getLocalPart(), a.getValue());
        }
    }

    void skipToEnd(String tagName) throws Exception {
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.END_ELEMENT:
                if (e.asEndElement().getName().getLocalPart().equals(tagName)) {
                    return;
                }
            }
        }
    }

    void writeToEnd(String tagName) throws Exception {
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                writeStartElement(e.asStartElement());
                break;
            case XMLEvent.COMMENT:
                Comment comment = (Comment) e;
                writer.writeComment(comment.getText());
                break;
            case XMLEvent.CHARACTERS:
                writer.writeCharacters(e.asCharacters().getData());
                break;
            case XMLEvent.END_ELEMENT:
                writer.writeEndElement();
                if (e.asEndElement().getName().getLocalPart().equals(tagName)) {
                    return;
                }
                break;
            default:
                throw new RuntimeException();
            }
        }
    }
}
