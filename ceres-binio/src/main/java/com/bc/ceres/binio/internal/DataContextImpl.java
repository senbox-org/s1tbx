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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;

public class DataContextImpl implements DataContext {
    private final DataFormat format;
    private final IOHandler handler;
    private volatile CompoundData data;

    public DataContextImpl(DataFormat format, IOHandler handler) {
        this.format = format;
        this.handler = handler;
    }

    @Override
    public DataFormat getFormat() {
        return format;
    }

    @Override
    public IOHandler getHandler() {
        return handler;
    }

    @Override
    public CompoundData getData() {
        if (data == null ) {
            synchronized (this) {
                if (data == null ) {
                    data = createData(0L);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundData createData() {
        return createData(0L);
    }

    @Override
    public CompoundData createData(long position) {
        return createData(format.getType(), position);
    }

    @Override
    public CompoundData createData(CompoundType type, long position) {
        return InstanceFactory.createCompound(this, null, type, position, format.getByteOrder());
    }

    @Override
    public CompoundData getData(long position) {
        return createData(format.getType(), position);
    }

    @Override
    public CompoundData getData(CompoundType type, long position) {
        return createData(type, position);
    }

    @Override
    public synchronized void dispose() {
        if (data != null) {
            try {
                data.flush();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}