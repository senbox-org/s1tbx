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

package com.bc.ceres.core.runtime.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;


class InstallInfo {

    private Date date;
    private String version;
    private String[] items;

    public InstallInfo(String[] items) {
        this.date = new Date();
        this.version = "1.0";
        this.items = items;
    }

    public Date getDate() {
        return date;
    }

    public String[] getItems() {
        return items;
    }

    public static InstallInfo read(Reader reader) throws IOException {
        try {
            InstallInfo installInfo = (InstallInfo) createXStream().fromXML(reader);
            if (installInfo.date == null) {
                installInfo.date = new Date();
            }
            if (installInfo.version == null) {
                installInfo.version = "1.0";
            }
            if (installInfo.items == null) {
                installInfo.items = new String[0];
            }
            return installInfo;
        } catch (XStreamException e) {
            IOException ioe = new IOException("Failed to read install info.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void write(Writer writer) throws IOException {
        try {
            this.date = new Date();
            createXStream().toXML(this, writer);
        } catch (XStreamException e) {
            IOException ioe = new IOException("Failed to write install info.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static XStream createXStream() {
        XStream xStream = new XStream();
        xStream.alias("install-info", InstallInfo.class);
        return xStream;
    }
}
