package org.alex73.android.arsc2;

import java.io.ByteArrayInputStream;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.reader.ChunkReader2;

public class ManifestInfo {
    private String packageName, versionName;

    public ManifestInfo(byte[] data) {
        ChunkReader2 rd = new ChunkReader2(new ByteArrayInputStream(data));

        Assert.assertTrue("XML chunk", rd.header.chunkType == ChunkHeader2.TYPE_DATA
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_XML);

        StringTable2 stringTable = new StringTable2();
        stringTable.read(rd.readChunk());

        while (!rd.isEOF()) {
            ChunkMapper m = rd.readChunk();
            switch (m.header.chunkType) {
            case ChunkHeader2.TYPE_XML_EVENT_START_TAG:
                ChunkXmlStartTag startTag = new ChunkXmlStartTag(m);
                if ("manifest".equals(startTag.getTagName(stringTable))) {
                    for (int i = 0; i < startTag.getAttributesCount(); i++) {
                        if ("versionName".equals(startTag.getAttributeName(i, stringTable))) {
                            versionName = startTag.getAttributeValueString(i, stringTable);
                        } else if ("package".equals(startTag.getAttributeName(i, stringTable))) {
                            packageName = startTag.getAttributeValueString(i, stringTable);
                        }
                    }
                    return;
                }
                break;
            }
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return versionName != null ? versionName : "unknown";
    }
}
