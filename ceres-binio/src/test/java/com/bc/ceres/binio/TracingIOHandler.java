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
