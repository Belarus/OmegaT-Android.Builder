package org.alex73.android.arsc2;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;

public class ChunkXmlStartTag {
    private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0, ATTRIBUTE_IX_NAME = 1,
            ATTRIBUTE_IX_VALUE_STRING = 2, ATTRIBUTE_IX_VALUE_TYPE = 3, ATTRIBUTE_IX_VALUE_DATA = 4,
            ATTRIBUTE_LENGHT = 5;

    int m_name;
    int[] m_attributes;

    public ChunkXmlStartTag(ChunkMapper rd) {
        Assert.assertTrue("ChunkXmlStartTag", rd.header.chunkType == ChunkHeader2.TYPE_XML_EVENT_START_TAG
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_XML_EVENT_START_TAG);
        
        int lineNumber = rd.readInt();
        /* 0xFFFFFFFF */rd.readInt();

        int m_namespaceUri = rd.readInt();
        m_name = rd.readInt();
        /* flags? */rd.readInt();
        int attributeCount = rd.readInt();
        int m_idAttribute = (attributeCount >>> 16) - 1;
        attributeCount &= 0xFFFF;
        int m_classAttribute = rd.readInt();
        int m_styleAttribute = (m_classAttribute >>> 16) - 1;
        m_classAttribute = (m_classAttribute & 0xFFFF) - 1;
        m_attributes = rd.readIntArray(attributeCount * ATTRIBUTE_LENGHT);
        for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < m_attributes.length;) {
            m_attributes[i] = (m_attributes[i] >>> 24);
            i += ATTRIBUTE_LENGHT;
        }
    }

    private final int getAttributeOffset(int index) {
        int offset = index * 5;
        if (offset >= m_attributes.length) {
            throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
        }
        return offset;
    }

    public String getTagName(StringTable2 stringTable) {
        return stringTable.getStrings().get(m_name).getRawString();
    }

    public int getAttributesCount() {
        return m_attributes.length / ATTRIBUTE_LENGHT;
    }

    public String getAttributeName(int index, StringTable2 stringTable) {
        int offset = getAttributeOffset(index);
        int name = m_attributes[offset + ATTRIBUTE_IX_NAME];
        if (name == -1) {
            return "";
        }
        return stringTable.getStrings().get(name).getRawString();
    }

    public String getAttributeValueString(int index, StringTable2 stringTable) {
        int offset = getAttributeOffset(index);
        int valueString = m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        if (valueString != -1) {
            return stringTable.getStrings().get(valueString).getRawString();
        } else {
//            int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
//            int valueData = m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
//
//            return TypedValue.coerceToString(valueType, valueData);
            return null;
        }
    }
}
