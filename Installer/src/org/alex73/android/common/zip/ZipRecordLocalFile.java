package org.alex73.android.common.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

public class ZipRecordLocalFile {
    int startPos;

    short versionNeededToExtract;
    short generalPurposeBitFlag;
    short compressionMethod;
    short lastModFileTime;
    short lastModFileDate;
    int crc32;
    int compressedSize;
    int uncompressedSize;
    short extraExtraFieldLength;
    byte[] fileName;
    int descCrc32;
    int descCompressedSize;
    int descUncompressedSize;
    byte[] compressedData;
    byte[] extraField;

    public ZipRecordLocalFile() {
    }

    public ZipRecordLocalFile(LEReader in) throws Exception {
        startPos = in.pos();

        // Local file header
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

        fileName = in.readFully(fileNameLength);

        extraField = in.readFully(extraFieldLength);

        // File data
        if (compressedSize == 0 && (generalPurposeBitFlag & 0x0008) != 0
                && compressionMethod == ZipEntry.DEFLATED) {
            // created by ZipOutputStream, without size on start
            byte[] bufin = new byte[512];
            byte[] bufout = new byte[512];
            Inflater inf = new Inflater(true);

            ByteArrayOutputStream compressed = new ByteArrayOutputStream(8192);

            int lenin = 0;
            while (true) {
                int lenInflated = inf.inflate(bufout, 0, bufout.length);
                if (lenInflated == 0) {
                    if (inf.finished() || inf.needsDictionary()) {
                        break;
                    }
                    if (inf.needsInput()) {
                        lenin = in.getInputStream().read(bufin);
                        compressed.write(bufin, 0, lenin);
                        inf.setInput(bufin, 0, lenin);
                    }
                }
            }
            byte[] co = compressed.toByteArray();

            int n = inf.getRemaining();
            if (n > 0) {
                in.pushback(bufin, lenin - n, n);
                compressedData = new byte[co.length - n];
                System.arraycopy(co, 0, compressedData, 0, compressedData.length);
            } else {
                compressedData = co;
            }
        } else {
            compressedData = in.readFully(compressedSize);
        }

        // Data descriptor
        // This descriptor exists only if bit 3 of the general purpose bit flag is set (see below)
        if ((generalPurposeBitFlag & 0x0008) != 0) {
            descCrc32 = in.readInt();
            if (descCrc32 == 0x08074b50) {
                // was signature
                descCrc32 = in.readInt();
            }
            descCompressedSize = in.readInt();
            descUncompressedSize = in.readInt();
        }
    }

    public String getFileName() throws Exception {
        return new String(fileName, "UTF-8");
    }

    public void writeHeader(LEWriter in) throws IOException {
        // Local file header
        in.writeShort(versionNeededToExtract);
        in.writeShort(generalPurposeBitFlag);
        in.writeShort(compressionMethod);
        in.writeShort(lastModFileTime);
        in.writeShort(lastModFileDate);
        in.writeInt(crc32);
        in.writeInt(compressedSize);
        in.writeInt(uncompressedSize);
        in.writeShort(fileName.length);
        in.writeShort(extraField.length + extraExtraFieldLength);

        in.writeFully(fileName);
    }

    public void writeFull(LEWriter wr) throws IOException {
        writeHeader(wr);

        wr.writeFully(extraField);
        wr.writeFully(new byte[extraExtraFieldLength]);

        wr.writeFully(compressedData);
        if ((generalPurposeBitFlag & 0x0008) != 0) {
            wr.writeInt(0x08074b50);
            wr.writeInt(descCrc32);
            wr.writeInt(descCompressedSize);
            wr.writeInt(descUncompressedSize);
        }
    }

    public void calcExtraExtra(int startPos) {
        int dataPos = startPos + 4 + 2 * 5 + 4 * 3 + 2 * 2 + fileName.length + extraField.length;
        extraExtraFieldLength = (short) (4 - (dataPos % 4));
        if (extraExtraFieldLength == 4) {
            extraExtraFieldLength = 0;
        }
    }
}
