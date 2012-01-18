import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.xmlpull.v1.XmlPullParser;

import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.XmlPullStreamDecoder;
import brut.androlib.res.util.ExtMXSerializer;

public class t {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream("C:/temp/a/androidmanifest.xml");
        OutputStream out = new FileOutputStream("C:/Temp/a/xml.xml");
        new XmlPullStreamDecoder(new AXmlResourceParser(), getResXmlSerializer()).decode(in, out);

        in = new FileInputStream("C:/temp/a/androidmanifest.xml");
        AXmlResourceParser parser = new AXmlResourceParser(in);
        while (true) {
            int token = parser.next();
            switch (token) {
            case XmlPullParser.START_TAG:
                System.out.println("start " + parser.getPrefix() + ":" + parser.getName() + " namespace="
                        + parser.getNamespace());
                for(int i=0;i<parser.getAttributeCount();i++) {
                    System.out.println("    attr="+parser.getAttributeName(i));
                    System.out.println("    attr="+parser.getAttributeValue(i));
                }
                break;
            case XmlPullParser.END_TAG:
                System.out.println("end " + parser.getName());
                break;
            }
        }
    }

    public static ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(serial.EXT_PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(serial.EXT_PROPERTY_SERIALIZER_LINE_SEPARATOR,
                System.getProperty("line.separator"));
        serial.setProperty(ExtMXSerializer.PROPERTY_DEFAULT_ENCODING, "UTF-8");
        return serial;
    }
}
