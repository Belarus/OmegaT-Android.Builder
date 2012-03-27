package org.alex73.android.common.zip;

import java.io.IOException;
import java.util.List;

public class ZipRecordEndOfCentralDirectory {
    short numberOfThisDisk;
    short numberOfTheDiskWithTheStartOfTheCentralDirectory;
    short totalNumberOfEntriesInTheCentralDirectoryOnThisDisk;
    short totalТumberOfEntriesInTheCentralDirectory;
    int sizeOfTheCentralDirectory;
    int offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber;
    byte[] zipFileComment;

    public ZipRecordEndOfCentralDirectory(LEReader in) throws Exception {
        // End of central directory record
        numberOfThisDisk = in.readShort();
        numberOfTheDiskWithTheStartOfTheCentralDirectory = in.readShort();
        totalNumberOfEntriesInTheCentralDirectoryOnThisDisk = in.readShort();
        totalТumberOfEntriesInTheCentralDirectory = in.readShort();
        sizeOfTheCentralDirectory = in.readInt();
        offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber = in.readInt();
        short zipFileCommentLength = in.readShort();
        zipFileComment = in.readFully(zipFileCommentLength);
    }

    public void write(LEWriter wr) throws IOException {
        wr.writeShort(numberOfThisDisk);
        wr.writeShort(numberOfTheDiskWithTheStartOfTheCentralDirectory);
        wr.writeShort(totalNumberOfEntriesInTheCentralDirectoryOnThisDisk);
        wr.writeShort(totalТumberOfEntriesInTheCentralDirectory);
        wr.writeInt(sizeOfTheCentralDirectory);
        wr.writeInt(offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber);
        wr.writeShort(zipFileComment.length);
        wr.writeFully(zipFileComment);
    }

    public void update(List<ZipRecordCentralDirectory> dir) {
        totalNumberOfEntriesInTheCentralDirectoryOnThisDisk = (short) dir.size();
        totalТumberOfEntriesInTheCentralDirectory = (short) dir.size();
        sizeOfTheCentralDirectory = 0;
        offsetOfStartOfCentralDirectoryWithRespectToTheStartingDiskNumber = -1;
        for (ZipRecordCentralDirectory d : dir) {
            sizeOfTheCentralDirectory += d.size();
        }
    }
}
