package org.alex73.android.arsc;

public class ManifestInfo {
    private String packageName, versionName;

    public ManifestInfo(byte[] data) {
        BytesReader rd = new BytesReader(data, 0, data.length);
        ChunkHeader2 headerXML = new ChunkHeader2(rd);
        Checks.expect(ChunkHeader2.TYPE_XML, headerXML.chunkType, "Invalid XML file");

        ChunkHeader2 headerStrings = new ChunkHeader2(rd);
        StringTable2 stringTable = new StringTable2(headerStrings, rd);

        while (!rd.isEOF()) {
            ChunkHeader2 nextChunkHeader = new ChunkHeader2(rd);
            switch (nextChunkHeader.chunkType) {
            case ChunkHeader2.TYPE_XML_START_TAG:
                ChunkXmlStartTag startTag = new ChunkXmlStartTag(nextChunkHeader, rd);
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
            default:
                rd.skip(nextChunkHeader.chunkSize - 8);
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
