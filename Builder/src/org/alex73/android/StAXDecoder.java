package org.alex73.android;

import java.io.StringReader;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class StAXDecoder extends StAXDecoderReader2 implements IDecoder {

    @Override
    public StyledString unmarshall(String str) throws Exception {
        XMLStreamReader rd = factory.createXMLStreamReader(new StringReader("<ROOT>" + str + "</ROOT>"));

        Assert.assertEquals("", XMLEvent.START_ELEMENT, rd.next());

        return read(rd);
    }
}
