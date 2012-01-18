package org.alex73.android.bel.zip;

import java.io.IOException;

public class ZipRecordCentralDirectory {
    short versionMadeBy;
    short versionNeededToExtract;
    short generalPurposeBitFlag;
    short compressionMethod;
    short lastModFileTime;
    short lastModFileDate;
    int crc32;
    int compressedSize;
    int uncompressedSize;
    short diskNumberStart;
    short internalFileAttributes;
    int externalFileAttributes;
    int relativeOffsetOfLocalHeader;

    byte[] fileName;
    byte[] extraField;
    byte[] fileComment;

    public int size() {
        return 4 + 2 * 6 + 4 * 3 + 2 * 5 + 4 * 2 + fileName.length + extraField.length + fileComment.length;
    }

    public ZipRecordCentralDirectory() {
    }

    public ZipRecordCentralDirectory(LEReader in) throws Exception {
        // Local file header
        versionMadeBy = in.readShort();
        versionNeededToExtract = in.readShort();
        generalPurposeBitFlag = in.readShort();
        compressionMethod = in.readShort();
        lastModFileTime = in.readShort();
        lastModFileDate = in.readShort();
        crc32 = in.readInt();
        compressedSize = in.readInt();
        uncompressedSize = in.readInt();
        short fileNameLength = in.readShort();
        short extraFieldLength = in.readShort();
        short fileCommentLength = in.readShort();
        diskNumberStart = in.readShort();
        internalFileAttributes = in.readShort();
        externalFileAttributes = in.readInt();
        relativeOffsetOfLocalHeader = in.readInt();

        fileName = in.readFully(fileNameLength);
        extraField = in.readFully(extraFieldLength);
        fileComment = in.readFully(fileCommentLength);
    }

    public String getFileName() throws Exception {
        return new String(fileName, "UTF-8");
    }

    public void write(LEWriter wr) throws IOException {
        wr.writeShort(versionMadeBy);
        wr.writeShort(versionNeededToExtract);
        wr.writeShort(generalPurposeBitFlag);
        wr.writeShort(compressionMethod);
        wr.writeShort(lastModFileTime);
        wr.writeShort(lastModFileDate);
        wr.writeInt(crc32);
        wr.writeInt(compressedSize);
        wr.writeInt(uncompressedSize);
        wr.writeShort(fileName.length);
        wr.writeShort(extraField.length);
        wr.writeShort(fileComment.length);
        wr.writeShort(diskNumberStart);
        wr.writeShort(internalFileAttributes);
        wr.writeInt(externalFileAttributes);
        wr.writeInt(relativeOffsetOfLocalHeader);

        wr.writeFully(fileName);
        wr.writeFully(extraField);
        wr.writeFully(fileComment);
    }
}
