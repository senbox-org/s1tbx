/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binio;

import com.bc.ceres.binio.internal.DataContextImpl;
import com.bc.ceres.binio.util.FileChannelIOHandler;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import com.bc.ceres.core.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A binary data format.
 */
public class DataFormat {
    private CompoundType type;
    private DataFormat basisFormat;
    private String name;
    private String version;
    private ByteOrder byteOrder;
    private final Map<String, Type> typeDefMap;

    public DataFormat() {
        this.typeDefMap = new HashMap<String, Type>(16);
    }

    public DataFormat(CompoundType type) {
        this(type, ByteOrder.BIG_ENDIAN);
    }

    public DataFormat(CompoundType type, ByteOrder byteOrder) {
        setType(type);
        setName(type.getName());
        setVersion("1.0.0");
        setByteOrder(byteOrder);
        this.typeDefMap = new HashMap<String, Type>(16);
    }

    /**
     * Creates a new random access file data context.
     *
     * @param file the file object
     * @param mode the access mode, one of <tt>"r"</tt>, <tt>"rw"</tt>, <tt>"rws"</tt>, or
     *             <tt>"rwd"</tt>. See also mode description in {@link RandomAccessFile#RandomAccessFile(java.io.File, String)}.
     * @return The context.
     * @throws FileNotFoundException If in read-only mode and the file could nt be found.
     */
    public DataContext createContext(File file, String mode) throws FileNotFoundException {
        Assert.notNull(file, "file");
        Assert.notNull(mode, "mode");
        final RandomAccessFile raf = new RandomAccessFile(file, mode);
        return new DataContextImpl(this, new RandomAccessFileIOHandler(raf)) {
            private boolean disposed;

            @Override
            public synchronized void dispose() {
                super.dispose();
                disposed = true;
                try {
                    raf.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
                if (!disposed) {
                    dispose();
                }
            }

        };
    }

    public DataContext createContext(RandomAccessFile raf) {
        Assert.notNull(raf, "raf");
        return new DataContextImpl(this, new RandomAccessFileIOHandler(raf));
    }

    public DataContext createContext(FileChannel fileChannel) {
        Assert.notNull(fileChannel, "fileChannel");
        return new DataContextImpl(this, new FileChannelIOHandler(fileChannel));
    }

    public DataContext createContext(IOHandler ioHandler) {
        Assert.notNull(ioHandler, "ioHandler");
        return new DataContextImpl(this, ioHandler);
    }

    public DataFormat getBasisFormat() {
        return basisFormat;
    }

    public void setBasisFormat(DataFormat basisFormat) {
        this.basisFormat = basisFormat;
    }

    public CompoundType getType() {
        return type;
    }

    public void setType(CompoundType type) {
        Assert.notNull(type, "type");
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Assert.notNull(name, "name");
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        Assert.notNull(version, "version");
        this.version = version;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        Assert.notNull(byteOrder, "byteOrder");
        this.byteOrder = byteOrder;
    }

    public boolean isTypeDef(String name) {
        Assert.notNull(name, "name");
        return typeDefMap.containsKey(name) || (basisFormat != null && basisFormat.isTypeDef(name));
    }

    public Type getTypeDef(String name) {
        Assert.notNull(name, "name");
        Type type = typeDefMap.get(name);
        if (type == null) {
            type = basisFormat != null ? basisFormat.getTypeDef(name) : null;
        }
        if (type == null) {
            throw new IllegalArgumentException(MessageFormat.format("Type definition ''{0}'' not found", name));
        }
        return type;
    }

    public void addTypeDef(String name, Type type) {
        Assert.notNull(name, "name");
        Assert.notNull(type, "type");
        Type oldType = typeDefMap.get(name);
        if (oldType != null && !oldType.equals(type)) {
            throw new IllegalArgumentException(MessageFormat.format("Type definition ''{0}'' already known as ''{1}''", name, oldType.getName()));
        }
        typeDefMap.put(name, type);
    }

    public Type removeTypeDef(String name) {
        Assert.notNull(name, "name");
        return typeDefMap.remove(name);
    }
}