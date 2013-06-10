package org.jlinda.core.io;


import org.jlinda.core.*;

import java.io.*;
import java.nio.ByteOrder;

abstract class FlatBinary implements DataReadersWriters {

//    private static Logger logger = Logger.getLogger(FlatBinary.class.getName());

    File file;
    String format;
    long size;
    public Window dataWindow;
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    DataInputStream inStream;
    DataOutputStream outStream;

    public abstract void readFromStream() throws FileNotFoundException;
    public abstract void writeToStream() throws FileNotFoundException;

    //// Setters for Streams ///
    public void setInStream() throws FileNotFoundException {
        inStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file.getAbsoluteFile())));
    }

    public void setOutStream() throws FileNotFoundException {
        outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile())));
    }

    //// Creating Files ////
    public void create() {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void create(File genericFile) {
        try {
            genericFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void create(String genericFileName) {
        this.create(new File(genericFileName));
    }

    //// Checkers ////
    public boolean checkExists() {
        return file.exists();
    }

    public boolean checkCanRead() throws FileNotFoundException {
        return file.canRead();
    }

    public boolean checkCanWrite() throws FileNotFoundException {
        return file.canWrite();
    }


    //// Geters and Setters ////

    public void setFile(File file) {
        this.file = file;
    }

    public void setFileName(String genericFileName) {
        this.file = new File(genericFileName);
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setDataWindow(Window dataWindow) {
        this.dataWindow = dataWindow;
    }

    public File getFile() {
        return file;
    }

    public String getFormat() {
        return format;
    }

    public long getSize() {
        return size;
    }

    public Window getDataWindow() {
        return dataWindow;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    //// Overrides ////
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlatBinaryDouble that = (FlatBinaryDouble) o;

        if (size != that.size) return false;
        if (dataWindow != null ? !dataWindow.equals(that.dataWindow) : that.dataWindow != null) return false;
        if (file != null ? !file.equals(that.file) : that.file != null) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (dataWindow != null ? dataWindow.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("FlatBinary");
        sb.append("{file=").append(file.getAbsoluteFile());
        sb.append(", format='").append(format).append('\'');
        sb.append(", size=").append(size);
        sb.append(", dataWindow=").append(dataWindow);
        sb.append('}');
        return sb.toString();
    }

}
