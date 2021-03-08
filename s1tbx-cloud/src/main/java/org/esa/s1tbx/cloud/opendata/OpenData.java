/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.cloud.opendata;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * OpenData interface for downloading
 */
public class OpenData {

    private final String odataRoot;
    private final Edm edm;
    private final String userName, password;

    private static final String APPLICATION_XML = "application/xml";
    private static final int MAX_DOWNLOAD_TRIES = 5;

    public OpenData(final String odataRoot, final String userName, final String password) throws IOException {
        this.odataRoot = odataRoot;
        this.userName = userName;
        this.password = password;

        try {
            final String odataMetaLink = odataRoot + "$metadata";
            final InputStream content = HTTPDownloader.connect(odataMetaLink, APPLICATION_XML, HTTPDownloader.HTTP_METHOD_GET,
                                                           userName, password);

            edm = EntityProvider.readMetadata(content, false);
            if (content != null) {
                content.close();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public File download(final String id, final String downloadURL, final File outputFolder, final String extension) throws IOException {
        final Entry entry = readEntry(odataRoot, APPLICATION_XML, "Products", id, "?");

        String fileName = entry.name;
        if(!fileName.endsWith(extension)) {
            fileName = fileName + extension;
        }
        final File outFile = new File(outputFolder, fileName);
        download(id, downloadURL, entry, outFile, ProgressMonitor.NULL);
        return outFile;
    }

    private Entry readEntry(final String serviceUri, final String contentType, final String entitySetName,
                            final String keyValue, final String params)
            throws IOException {
        try {
            final EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();

            final InputStream content = HTTPDownloader.connect(serviceUri + entitySetName + "('" + keyValue + "')" + params,
                                                           contentType, HTTPDownloader.HTTP_METHOD_GET,
                                                           userName, password);
            ODataEntry oDataEntry = EntityProvider.readEntry(contentType, entityContainer.getEntitySet(entitySetName),
                                                                   content, EntityProviderReadProperties.init().build());
            return new Entry(oDataEntry);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void download(final String id, final String downloadURL, final Entry entry, final File outFile, final ProgressMonitor pm) {

        //final String download = odataRoot+"Products('" + id + "')" + "/$value";

        int tries = 1;
        HTTPDownloader.EntryFileProperty entryFp = null;
        while ((entryFp == null || entryFp.getSize() != entry.contentLength)) {

            entryFp = HTTPDownloader.getEntryFilePropertyFromUrlString(downloadURL, outFile,
                    entry.contentLength, entry.contentType, userName, password, pm);

            if (entryFp != null && entryFp.getSize() == entry.contentLength) {
                break;
            } else {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tries > MAX_DOWNLOAD_TRIES && (entryFp == null || entryFp.getSize() != entry.contentLength)) {
                    break;
                }
                tries++;
            }
        }
    }

    public static class Entry {
        public final Long contentLength;
        public final String name;
        public final String contentType;

        public Entry(final ODataEntry oDataEntry) {
            final Map<String, Object> propMap = oDataEntry.getProperties();

            contentLength = Long.parseLong(propMap.get("ContentLength").toString());
            contentType = propMap.get("ContentType").toString();

            name = propMap.get("Name").toString();
        }
    }
}
