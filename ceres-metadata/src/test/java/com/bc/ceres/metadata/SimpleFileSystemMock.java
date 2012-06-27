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

package com.bc.ceres.metadata;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


class SimpleFileSystemMock implements SimpleFileSystem {

    private Map<String, Reader> readerMap = new HashMap<String, Reader>();
    private Map<String, Writer> writerMap = new HashMap<String, Writer>();
    private Map<String, String[]> listMap = new HashMap<String, String[]>();

    @Override
    public Reader getReader(String path) throws IOException {
        return readerMap.get(path);
    }

    @Override
    public Writer getWriter(String path) throws IOException {
        return writerMap.get(path);
    }

    @Override
    public String[] list(String path) throws IOException {
        return this.listMap.get(path);
    }

    void setReader(String name, Reader reader) {
        readerMap.put(name, reader);
    }

    void setList(String dirName, String... files) {
        listMap.put(dirName, files);
    }

    void setWriter(String name, Writer writer) {
        writerMap.put(name, writer);
    }
}
