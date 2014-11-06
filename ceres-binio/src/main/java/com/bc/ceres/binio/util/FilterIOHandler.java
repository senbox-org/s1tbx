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

package com.bc.ceres.binio.util;

import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;

public abstract class FilterIOHandler implements IOHandler {
    private final IOHandler delegate;

    protected FilterIOHandler(IOHandler delegate) {
        this.delegate = delegate;
    }

    public final IOHandler getDelegate() {
        return delegate;
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        delegate.read(context, data, position);
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        delegate.write(context, data, position);
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        return delegate.getMaxPosition();
    }
}