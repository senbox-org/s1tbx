/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.resource;

import java.io.FileReader;
import java.io.IOException;

/**
 * A [@link Resource} reading its content from a @{link File}.
 */
public class FileResource extends Resource {

    public FileResource(String path) {
        this(path, null);
    }

    public FileResource(String path, Resource origin) {
        super(path, origin);
    }

    @Override
    protected String read() {
        String path = getPath();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(path);
            return readText(fileReader);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read from path: " + path, e);
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to read from path: " + path, e);
            }
        }
    }

}
