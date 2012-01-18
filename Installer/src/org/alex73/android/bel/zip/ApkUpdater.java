package org.alex73.android.bel.zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;

/**
 * Zip specification described on http://www.pkware.com/documents/casestudies/APPNOTE.TXT
 */
public class ApkUpdater {
    static byte[] RESOURCES_NAME_OLD = "old_resu_.arsc".getBytes();

    static final String RESOURCES_NAME = "resources.arsc";
    static final String MARK_NAME = "i18n.bel";

    public void append(File apkFile, byte[] mark, byte[] newResources) throws Exception {
        ZipReader zip = new ZipReader(new RandomAccessFile(apkFile, "rw"));
        LEReader in = zip.getReader();
        try {
            ZipRecordLocalFile oldResourceFile = null;
            int dirStart = -1;
            ZipRecordEndOfCentralDirectory end = null;
            List<ZipRecordCentralDirectory> dir = new ArrayList<ZipRecordCentralDirectory>();

            while (end == null) {
                int signature = in.readInt();
                switch (signature) {
                case 0x04034b50:
                    ZipRecordLocalFile f = new ZipRecordLocalFile(in);
                    if (RESOURCES_NAME.equals(f.getFileName())) {
                        oldResourceFile = f;
                    }
                    break;
                case 0x02014b50:
                    if (dir.size() == 0) {
                        // first entry
                        dirStart = in.pos() - 4;
                    }
                    ZipRecordCentralDirectory d = new ZipRecordCentralDirectory(in);
                    dir.add(d);
                    break;
                case 0x06054b50:
                    end = new ZipRecordEndOfCentralDirectory(in);
                    break;
                default:
                    throw new Exception("Invalid signature: " + Integer.toHexString(signature));
                }
            }

            if (dirStart <= 0 || dir.isEmpty() || oldResourceFile == null) {
                throw new Exception("Invalid directory");
            }

            // change
            oldResourceFile.fileName = RESOURCES_NAME_OLD;
            for (ZipRecordCentralDirectory d : dir) {
                if (RESOURCES_NAME.equals(d.getFileName())) {
                    d.fileName = RESOURCES_NAME_OLD;
                }
            }
            generateNewEntries(mark, newResources, oldResourceFile.compressionMethod == ZipEntry.DEFLATED);
            dir.add(dData);
            dir.add(dMark);
            end.update(dir);

            TempWriter appendWriter = new TempWriter();

            dData.relativeOffsetOfLocalHeader = dirStart + appendWriter.size();
            fData.calcExtraExtra(dData.relativeOffsetOfLocalHeader);

            appendWriter.writeInt(0x04034b50);
            fData.writeFull(appendWriter);

            dMark.relativeOffsetOfLocalHeader = dirStart + appendWriter.size();
            fMark.calcExtraExtra(dMark.relativeOffsetOfLocalHeader);

            appendWriter.writeInt(0x04034b50);
            fMark.writeFull(appendWriter);

            end.offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber = dirStart
                    + appendWriter.size();

            for (ZipRecordCentralDirectory d : dir) {
                appendWriter.writeInt(0x02014b50);
                d.write(appendWriter);
            }
            appendWriter.writeInt(0x06054b50);
            end.write(appendWriter);

            byte[] appendBlock = appendWriter.toByteArray();
            appendWriter = null;

            in = null; // больш не карыстаемся
            LEWriter wr = zip.getWriter();

            // write new resources
            wr.seek(dirStart);
            wr.writeFully(appendBlock);

            // update old resources
            wr.seek(oldResourceFile.startPos);
            oldResourceFile.writeHeader(wr);
        } finally {
            zip.close();
        }
    }

    public void replace(File apkFile, File outFile, byte[] mark, byte[] newResources) throws Exception {
        ZipReader zip = new ZipReader(new RandomAccessFile(apkFile, "r"));
        ZipReader zipOut = new ZipReader(new RandomAccessFile(outFile, "rw"));
        LEReader in = zip.getReader();
        LEWriter o = zipOut.getWriter();
        try {
            ZipRecordEndOfCentralDirectory end = null;
            Map<String, Integer> oldOffsets = new HashMap<String, Integer>();
            Map<String, Integer> newOffsets = new HashMap<String, Integer>();
            List<ZipRecordCentralDirectory> dir = new ArrayList<ZipRecordCentralDirectory>();

            boolean oldResourceCompressed = false;
            while (end == null) {
                int pos = in.pos();
                int signature = in.readInt();
                switch (signature) {
                case 0x04034b50:
                    ZipRecordLocalFile f = new ZipRecordLocalFile(in);
                    oldOffsets.put(f.getFileName(), pos);
                    if (RESOURCES_NAME.equals(f.getFileName())) {
                        oldResourceCompressed = f.compressionMethod == ZipEntry.DEFLATED;
                    } else if (MARK_NAME.equals(f.getFileName())) {
                    } else {
                        newOffsets.put(f.getFileName(), o.pos());
                        o.writeInt(0x04034b50);
                        f.writeFull(o);
                    }
                    break;
                case 0x02014b50:
                    ZipRecordCentralDirectory d = new ZipRecordCentralDirectory(in);
                    if (RESOURCES_NAME.equals(d.getFileName())) {
                    } else if (MARK_NAME.equals(d.getFileName())) {
                    } else {
                        dir.add(d);
                    }
                    int storedPos = oldOffsets.get(d.getFileName());
                    check(storedPos == d.relativeOffsetOfLocalHeader, "Wrong offset in zip dir");
                    break;
                case 0x06054b50:
                    end = new ZipRecordEndOfCentralDirectory(in);
                    break;
                default:
                    throw new Exception("Invalid signature: " + Integer.toHexString(signature));
                }
            }

            in = null; // больш не карыстаемся

            generateNewEntries(mark, newResources, oldResourceCompressed);

            // change dir
            dir.add(dData);
            dir.add(dMark);
            end.update(dir);

            int opos = o.pos();
            newOffsets.put(fData.getFileName(), opos);
            fData.calcExtraExtra(opos);

            o.writeInt(0x04034b50);
            fData.writeFull(o);

            opos = o.pos();
            newOffsets.put(fMark.getFileName(), opos);
            fMark.calcExtraExtra(opos);

            o.writeInt(0x04034b50);
            fMark.writeFull(o);

            end.offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber = o.pos();

            for (ZipRecordCentralDirectory d : dir) {
                d.relativeOffsetOfLocalHeader = newOffsets.get(d.getFileName());
                o.writeInt(0x02014b50);
                d.write(o);
            }
            o.writeInt(0x06054b50);
            end.write(o);
        } finally {
            zip.close();
            zipOut.close();
        }
    }

    private ZipRecordLocalFile fData, fMark;
    private ZipRecordCentralDirectory dData, dMark;

    private void generateNewEntries(byte[] mark, byte[] newResources, boolean allowPackResources)
            throws Exception {

        byte[] newResourcesCompressed = allowPackResources ? deflate(newResources) : newResources;
        fData = new ZipRecordLocalFile();
        fData.versionNeededToExtract = (short) (allowPackResources ? 20 : 10);
        fData.generalPurposeBitFlag = 0;
        fData.compressionMethod = (short) (allowPackResources ? ZipEntry.DEFLATED : ZipEntry.STORED);
        fData.lastModFileTime = 0;
        fData.lastModFileDate = 0;
        fData.crc32 = crc32(newResources);
        fData.compressedSize = newResourcesCompressed.length;
        fData.uncompressedSize = newResources.length;
        fData.fileName = RESOURCES_NAME.getBytes();
        fData.extraField = new byte[0];
        fData.compressedData = newResourcesCompressed;

        dData = new ZipRecordCentralDirectory();
        dData.versionMadeBy = fData.versionNeededToExtract;
        dData.versionNeededToExtract = fData.versionNeededToExtract;
        dData.generalPurposeBitFlag = fData.generalPurposeBitFlag;
        dData.compressionMethod = fData.compressionMethod;
        dData.lastModFileTime = fData.lastModFileTime;
        dData.lastModFileDate = fData.lastModFileDate;
        dData.crc32 = fData.crc32;
        dData.compressedSize = fData.compressedSize;
        dData.uncompressedSize = fData.uncompressedSize;
        dData.diskNumberStart = 0;
        dData.internalFileAttributes = 0;
        dData.externalFileAttributes = 0;
        dData.relativeOffsetOfLocalHeader = -1;
        dData.fileName = fData.fileName;
        dData.extraField = new byte[0];
        dData.fileComment = new byte[0];

        fMark = new ZipRecordLocalFile();
        fMark.versionNeededToExtract = 10;
        fMark.generalPurposeBitFlag = 0;
        fMark.compressionMethod = (short) ZipEntry.STORED;
        fMark.lastModFileTime = 0;
        fMark.lastModFileDate = 0;
        fMark.crc32 = crc32(mark);
        fMark.compressedSize = mark.length;
        fMark.uncompressedSize = mark.length;
        fMark.fileName = MARK_NAME.getBytes();
        fMark.extraField = new byte[0];
        fMark.compressedData = mark;

        dMark = new ZipRecordCentralDirectory();
        dMark.versionMadeBy = fMark.versionNeededToExtract;
        dMark.versionNeededToExtract = fMark.versionNeededToExtract;
        dMark.generalPurposeBitFlag = fMark.generalPurposeBitFlag;
        dMark.compressionMethod = fMark.compressionMethod;
        dMark.lastModFileTime = fMark.lastModFileTime;
        dMark.lastModFileDate = fMark.lastModFileDate;
        dMark.crc32 = fMark.crc32;
        dMark.compressedSize = fMark.compressedSize;
        dMark.uncompressedSize = fMark.uncompressedSize;
        dMark.diskNumberStart = 0;
        dMark.internalFileAttributes = 0;
        dMark.externalFileAttributes = 0;
        dMark.relativeOffsetOfLocalHeader = -1;
        dMark.fileName = fMark.fileName;
        dMark.extraField = new byte[0];
        dMark.fileComment = new byte[0];
    }

    protected int crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length);
        return (int) crc.getValue();
    }

    protected byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DeflaterOutputStream out = new DeflaterOutputStream(buffer, new Deflater(
                Deflater.DEFAULT_COMPRESSION, true));
        out.write(data);
        out.close();
        return buffer.toByteArray();
    }

    protected void check(boolean check, String text) {
        if (!check) {
            throw new RuntimeException(text);
        }
    }
}
