package com.bc.ceres.binio;

import com.bc.ceres.binio.util.FilterIOHandler;

import java.io.IOException;

public class TracingIOHandler extends FilterIOHandler {
    private final StringBuilder trace = new StringBuilder(512);

    public TracingIOHandler(IOHandler ioHandler) {
        super(ioHandler);
    }

    public void reset() {
        trace.setLength(0);
    }

    public String getTrace() {
        return trace.toString();
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        trace.append("R(").append(position).append(",").append(data.length).append(")");
        getDelegate().read(context, data, position);
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        trace.append("W(").append(position).append(",").append(data.length).append(")");
        getDelegate().write(context, data, position);
    }

    @Override
    public String toString() {
        return getTrace();
    }
}
