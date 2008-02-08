package org.esa.beam.dataio.shapefile;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DBaseTable {
    private ImageInputStream stream;
    private int version;
    private String lastModified;
    private int recordCount;
    private int headerSize;
    private int recordSize;
    private Field[] fields;
    private long recordStartPosition;
    private static final int FIELDS_TERMINATOR = 0x0D;

    public DBaseTable(File file) throws IOException {
        this(new FileImageInputStream(file));
    }

    public DBaseTable(ImageInputStream stream) throws IOException {
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        this.stream = stream;
        version = stream.readUnsignedByte();
        int year = stream.readUnsignedByte() + 1900;
        int month = stream.readUnsignedByte();
        int dayOfMonth = stream.readUnsignedByte();
        lastModified = year + "-" + (month < 10 ? "0" : "") + month + "-" + (dayOfMonth < 10 ? "0" : "") + dayOfMonth;
        recordCount = stream.readInt();
        headerSize = stream.readUnsignedShort();
        recordSize = stream.readUnsignedShort();
        stream.skipBytes(20);

        ArrayList<Field> columnList = new ArrayList<Field>(32);
        while (true) {
            long streamPos = stream.getStreamPosition();
            int firstByte = stream.readUnsignedByte();
            if (firstByte == FIELDS_TERMINATOR) {
                break;
            }
            stream.seek(streamPos);
            columnList.add(new Field(stream));
        }
        fields = columnList.toArray(new Field[columnList.size()]);

        int fieldLengthSum = 0;
        for (Field field : fields) {
            fieldLengthSum += field.getLength();
        }
        if (fieldLengthSum + 1 != getRecordSize()) {
            throw new IOException("Corrupted dBase file.");
        }

        stream.seek((long) headerSize);
        recordStartPosition = stream.getStreamPosition();
    }

    public static void main(String[] args) {
        for (String filePath : args) {
            System.out.println("File " + filePath + ":");
            try {
                DBaseTable dBaseTable = new DBaseTable(new File(filePath));
                dBaseTable.dump(System.out);
                dBaseTable.close();
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }

        }
    }
    public void dump(PrintStream w) {
        for (Field field : fields) {
            String fieldName = field.getName();
            w.print('|' + fieldName);
        }
        w.println('|');
        try {
            for (int i = 0; i < recordCount; i++) {
                String[] record = readRecord(i);
                for (String value : record) {
                    w.print('|' + value);
                }
                w.println('|');
            }
        } catch (IOException e) {
            w.println("Error: " + e.getMessage());
        }
    }

    public String[] readRecord(int index) throws IOException {
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        stream.seek(recordStartPosition + index * recordSize);
        String[] record = new String[fields.length];
        stream.readUnsignedByte(); // Record Deleted Flag
        for (int i = 0; i < fields.length; i++) {
            record[i] = readString(stream, fields[i].getLength()).trim();
        }
        return record;
    }

    public void close() throws IOException {
        stream.close();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public void setRecordSize(int recordSize) {
        this.recordSize = recordSize;
    }

    public Field[] getFields() {
        return fields;
    }

    public void setFields(Field[] fields) {
        this.fields = fields;
    }

    private static String readString(ImageInputStream stream, int n) throws IOException {
        byte[] b = new byte[n];
        int m = stream.read(b);
        return new String(b);
    }

    public static class Field {
        private String name;
        private char type;
        private int length;
        private int decimalCount;
        private boolean indexFlag;

        Field(ImageInputStream stream) throws IOException {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            name = readString(stream, 10).trim();
            stream.skipBytes(1);
            type = (char) stream.readByte();
            stream.skipBytes(4);
            length = stream.readUnsignedByte();
            decimalCount = stream.readUnsignedByte();
            stream.skipBytes(13);
            indexFlag = stream.readUnsignedByte() != 0;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(255);
            s.append(getName());
            s.append(',');
            s.append(getType());
            s.append(',');
            s.append(getLength());
            s.append(',');
            s.append(getDecimalCount());
            s.append(',');
            s.append(isIndexFlag());
            return s.toString();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public char getType() {
            return type;
        }

        public void setType(char type) {
            this.type = type;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getDecimalCount() {
            return decimalCount;
        }

        public void setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
        }

        public boolean isIndexFlag() {
            return indexFlag;
        }

        public void setIndexFlag(boolean indexFlag) {
            this.indexFlag = indexFlag;
        }
    }

}
