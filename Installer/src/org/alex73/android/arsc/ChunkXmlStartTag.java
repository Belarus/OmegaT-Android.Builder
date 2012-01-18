package org.alex73.android.arsc;

public class ChunkXmlStartTag extends BaseChunk2 {
    private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0, ATTRIBUTE_IX_NAME = 1,
            ATTRIBUTE_IX_VALUE_STRING = 2, ATTRIBUTE_IX_VALUE_TYPE = 3, ATTRIBUTE_IX_VALUE_DATA = 4,
            ATTRIBUTE_LENGHT = 5;

    int m_name;
    int[] m_attributes;

    public ChunkXmlStartTag(ChunkHeader2 header, BytesReader reader) {
        super(header, reader);
        Checks.expect(ChunkHeader2.TYPE_XML_START_TAG, header.chunkType, "Invalid ChunkXmlStartTag");

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
        m_attributes = rd.readIntArr(attributeCount * ATTRIBUTE_LENGHT);
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
        return stringTable.getString(m_name);
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
        return stringTable.getString(name);
    }

    public String getAttributeValueString(int index, StringTable2 stringTable) {
        int offset = getAttributeOffset(index);
        int valueString = m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        if (valueString != -1) {
            return stringTable.getString(valueString);
        } else {
//            int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
//            int valueData = m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
//
//            return TypedValue.coerceToString(valueType, valueData);
            return null;
        }
    }
    
    
}
