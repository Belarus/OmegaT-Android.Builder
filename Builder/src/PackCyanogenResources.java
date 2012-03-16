import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.omegat.util.OConsts;

import android.control.App;
import android.control.Cyanogen;
import android.control.Translation;

/**
 * Export target files to cyanogen.
 */
public class PackCyanogenResources {
    static String projectPath = "../../Android.OmegaT/Android/";
    static XMLInputFactory INPUT_FACTORY;
    static XMLOutputFactory OUTPUT_FACTORY;

    public static void main(String[] args) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(Translation.class);
        Translation translationInfo = (Translation) ctx.createUnmarshaller().unmarshal(
                new File(projectPath + "../translation.xml"));

        INPUT_FACTORY = XMLInputFactory.newInstance();
        INPUT_FACTORY.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        OUTPUT_FACTORY = XMLOutputFactory.newInstance();

        // process cyanogen resources
        for (App app : translationInfo.getApp()) {
            File translatedDir = new File(projectPath + "/target/" + app.getDirName());
            for (Cyanogen cy : app.getCyanogen()) {
                String version = "cyanogen-"+args[1];
                if (cy.getOutSuffix() != null) {
                    version = cy.getOutSuffix() + '-' + version;
                }

                System.out.println("From cyanogen " + cy.getSrc() + " version=" + version + " into "
                        + translatedDir.getAbsolutePath());

                translatedDir.mkdirs();
                process(new File(args[0], cy.getSrc() + "/values/strings.xml"), new File(translatedDir,
                        "strings_" + version + ".xml"), new File(args[0], cy.getSrc()
                        + "/values-be/strings.xml"));
                process(new File(args[0], cy.getSrc() + "/values/arrays.xml"), new File(translatedDir,
                        "arrays_" + version + ".xml"), new File(args[0], cy.getSrc()
                        + "/values-be/arrays.xml"));
                process(new File(args[0], cy.getSrc() + "/values/plurals.xml"), new File(translatedDir,
                        "plurals_" + version + ".xml"), new File(args[0], cy.getSrc()
                        + "/values-be/plurals.xml"));
            }
        }
    }

    static XMLEventReader xmlIn;
    static XMLStreamWriter xmlOut;
    static Map<String, String> srcData, transData;

    static void process(File source, File translated, File target) throws Exception {
        if (!source.exists()) {
            return;
        }
        System.out.println("        " + translated.getAbsolutePath() + " -> " + target.getAbsolutePath());

        srcData = loadTexts(source);
        transData = loadTexts(translated);

        InputStream in = new BufferedInputStream(new FileInputStream(translated));
        xmlIn = INPUT_FACTORY.createXMLEventReader(in);

        target.getParentFile().mkdirs();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        xmlOut = OUTPUT_FACTORY.createXMLStreamWriter(out, OConsts.UTF8);

        boolean started = false;
        boolean hasTranslations = false;
        while (xmlIn.hasNext()) {
            XMLEvent e = xmlIn.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                StartDocument dStart = (StartDocument) e;
                xmlOut.writeStartDocument(dStart.getCharacterEncodingScheme(), dStart.getVersion());
                xmlOut.writeCharacters("\n");
                break;
            case XMLEvent.START_ELEMENT:
                started = true;
                StartElement eStart = (StartElement) e;
                switch (eStart.getName().getLocalPart()) {
                case "resources":
                    xmlOut.writeCharacters("\n");
                    writeStartTag(eStart);
                    break;
                case "string-array":
                case "array":
                case "integer-array":
                case "plurals":
                    if (needTranslate(eStart)) {
                        hasTranslations = true;
                        xmlOut.writeCharacters("\n    ");
                        writeStartTag(eStart);
                    } else {
                        skipSegment();
                    }
                    break;
                case "string":
                case "color":
                    if (needTranslate(eStart)) {
                        hasTranslations = true;
                        xmlOut.writeCharacters("\n    ");
                        writeStartTag(eStart);
                        copySegment();
                        xmlOut.writeEndElement();
                    } else {
                        skipSegment();
                    }
                    break;
                case "item":
                    xmlOut.writeCharacters("\n        ");
                    writeStartTag(eStart);
                    copySegment();
                    xmlOut.writeEndElement();
                    break;
                case "skip":
                case "eat-comment":
                    e = xmlIn.nextEvent();
                    if (!e.isEndElement()) {
                        throw new Exception("Invalid skip");
                    }
                    break;
                default:
                    throw new Exception("Unknown tag: " + eStart);
                }
                break;
            case XMLEvent.END_ELEMENT:
                switch (e.asEndElement().getName().getLocalPart()) {
                case "string-array":
                case "array":
                case "integer-array":
                case "plurals":
                    xmlOut.writeCharacters("\n    ");
                    break;
                case "resources":
                    xmlOut.writeCharacters("\n");
                    break;
                }
                xmlOut.writeEndElement();
                break;
            case XMLEvent.END_DOCUMENT:
                xmlOut.writeEndDocument();
                break;
            case XMLEvent.COMMENT:
                if (!started) {
                    Comment com = (Comment) e;
                    xmlOut.writeComment(com.getText());
                }
                break;
            case XMLEvent.CHARACTERS:
                break;
            default:
                throw new Exception("Unknown event: " + e);
            }
        }

        xmlIn.close();
        in.close();

        xmlOut.close();
        out.close();

        if (!hasTranslations) {
            target.delete();
        }
    }

    static boolean needTranslate(StartElement e) {
        Attribute tr = e.getAttributeByName(new QName("translatable"));
        if (tr != null && "false".equals(tr.getValue())) {
            return false;
        }

        String key = createKey(e);
        String src = srcData.get(key);
        String trans = transData.get(key);
        if (src.equals(trans)) {
            return false;
        }

        return true;
    }

    static void writeStartTag(StartElement eStart) throws Exception {
        QName q = eStart.getName();
        xmlOut.writeStartElement(q.getPrefix(), q.getLocalPart(), q.getNamespaceURI());

        for (Iterator<Namespace> it = eStart.getNamespaces(); it.hasNext();) {
            Namespace nm = it.next();
            xmlOut.writeNamespace(nm.getPrefix(), nm.getNamespaceURI());
        }

        for (Iterator<Attribute> it = eStart.getAttributes(); it.hasNext();) {
            Attribute a = it.next();
            QName qa = a.getName();
            xmlOut.writeAttribute(qa.getPrefix(), qa.getNamespaceURI(), qa.getLocalPart(), a.getValue());
        }
    }

    static void skipSegment() throws Exception {
        int level = 0;
        do {
            XMLEvent e = xmlIn.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                level++;
                break;
            case XMLEvent.END_ELEMENT:
                level--;
                break;
            }
        } while (level >= 0);
    }

    static void copySegment() throws Exception {
        int level = 0;
        do {
            XMLEvent e = xmlIn.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                level++;
                StartElement eStart = (StartElement) e;
                QName q = eStart.getName();
                xmlOut.writeStartElement(q.getPrefix(), q.getLocalPart(), q.getNamespaceURI());
                break;
            case XMLEvent.END_ELEMENT:
                level--;
                if (level >= 0) {
                    xmlOut.writeEndElement();
                }
                break;
            case XMLEvent.CHARACTERS:
                Characters cha = (Characters) e;
                xmlOut.writeCharacters(cha.getData());
                break;
            default:
                throw new Exception("Unknown event: " + e);
            }
        } while (level >= 0);
    }

    static String createKey(StartElement eStart) {
        String key = eStart.getName().getLocalPart();
        key += eStart.getAttributeByName(new QName("name")).getValue();
        Attribute p = eStart.getAttributeByName(new QName("product"));
        if (p != null) {
            key += '/' + p.getValue();
        }
        return key;
    }

    static Map<String, String> loadTexts(File inFile) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        xmlIn = INPUT_FACTORY.createXMLEventReader(in);

        boolean started = false;
        while (xmlIn.hasNext()) {
            XMLEvent e = xmlIn.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                break;
            case XMLEvent.START_ELEMENT:
                started = true;
                StartElement eStart = (StartElement) e;
                switch (eStart.getName().getLocalPart()) {
                case "resources":
                    break;
                case "string-array":
                case "array":
                case "integer-array":
                case "plurals":
                case "string":
                case "color":
                    String key = createKey(eStart);
                    if (result.containsKey(key)) {
                        throw new Exception("Already exist: " + key);
                    }
                    result.put(key, getElementAsText());
                    break;
                case "skip":
                case "eat-comment":
                    e = xmlIn.nextEvent();
                    if (!e.isEndElement()) {
                        throw new Exception("Invalid skip");
                    }
                    break;
                default:
                    throw new Exception("Unknown tag: " + eStart);
                }
                break;
            case XMLEvent.END_ELEMENT:
                break;
            case XMLEvent.END_DOCUMENT:
                break;
            case XMLEvent.COMMENT:
                break;
            case XMLEvent.CHARACTERS:
                break;
            default:
                throw new Exception("Unknown event: " + e);
            }
        }

        xmlIn.close();
        in.close();

        return result;
    }

    static String getElementAsText() throws Exception {
        StringBuilder out = new StringBuilder();
        int level = 0;
        do {
            XMLEvent e = xmlIn.nextEvent();

            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                level++;
                out.append('<').append(e.asStartElement().getName().getLocalPart()).append('>');
                break;
            case XMLEvent.END_ELEMENT:
                level--;
                if (level >= 0) {
                    out.append('<').append(e.asEndElement().getName().getLocalPart()).append('>');
                }
                break;
            case XMLEvent.CHARACTERS:
                Characters cha = (Characters) e;
                out.append(cha.getData());
                break;
            case XMLEvent.COMMENT:
                break;
            default:
                throw new Exception("Unknown event: " + e);
            }
        } while (level >= 0);
        return out.toString();
    }
}
