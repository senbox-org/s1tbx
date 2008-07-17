package com.bc.ceres.binio;

import com.bc.ceres.binio.internal.InstanceFactory;

/**
 * The context provides the means to read from or write to a random access stream.
 */
public class IOContext {
    private final Format format;
    private final IOHandler handler;

    public IOContext(Format format, IOHandler handler) {
        this.format = format;
        this.handler = handler;
    }

    public Format getFormat() {
        return format;
    }

    public IOHandler getHandler() {
        return handler;
    }

    public CompoundData getData() {
        return getData(0L);
    }

    public CompoundData getData(long position) {
        return InstanceFactory.createCompound(this, null, format.getType(), position, format.getByteOrder());
    }
}