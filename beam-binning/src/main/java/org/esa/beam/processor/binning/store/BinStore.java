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

package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;

import org.esa.beam.processor.binning.database.Bin;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 15.07.2005
 * Time: 15:21:42
 * To change this template use File | Settings | File Templates.
 */
public interface BinStore {

    public void write(Point rowCol, Bin bin) throws IOException;

    public void read(Point rowCol, Bin bin) throws IOException;

    public void flush() throws IOException;

    public void close() throws IOException;

    public void delete() throws IOException;
}
