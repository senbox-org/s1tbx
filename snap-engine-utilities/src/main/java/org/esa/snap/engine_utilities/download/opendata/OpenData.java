/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.download.opendata;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Credentials;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * OpenData interface for downloading
 */
public class OpenData {

    private final String host;
    private final String odataRoot;
    private final Credentials.CredentialInfo credentialInfo;
    private final HTTPDownloader downloader;
    private final Edm edm;

    private static final String APPLICATION_XML = "application/xml";
    private static final int MAX_DOWNLOAD_TRIES = 5;

    public OpenData(final String host, final String odataRoot) throws IOException {
        this.host = host;
        this.odataRoot = odataRoot;
        this.credentialInfo = getCredentialInfo();
        this.downloader = new HTTPDownloader();

        try {
            final String odataMetaLink = odataRoot + "$metadata";
            final InputStream content = downloader.connect(odataMetaLink, APPLICATION_XML, HTTPDownloader.HTTP_METHOD_GET,
                                                           credentialInfo.getUser(), credentialInfo.getPassword());

            edm = EntityProvider.readMetadata(content, false);
            if (content != null) {
                content.close();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Entry getEntryByID(final String id) throws IOException {
        return readEntry(odataRoot, APPLICATION_XML, "Products", id, "?");
    }

    public void getManifest(final String id, final Entry entry, final File outputFolder, final ProgressMonitor pm) throws IOException {

        int tries = 1;
        HTTPDownloader.EntryFileProperty entryFp = null;
        while ((entryFp == null || entryFp.getSize() != entry.contentLength)) {

            final String manifest = "/Nodes('" + entry.name + ".SAFE')/Nodes('manifest.safe')/\\";
            final String downloadURL = odataRoot+"Products('" + id + "')" + manifest + "$value?";

            SystemUtils.LOG.info(downloadURL);

            entryFp = downloader.getEntryFilePropertyFromUrlString(downloadURL,
                                                                   entry.fileName, entry.contentLength, entry.contentType,
                                                                   outputFolder,
                                                                   credentialInfo.getUser(), credentialInfo.getPassword(),
                                                                   pm);

            if (entryFp != null && entryFp.getSize() == entry.contentLength) {
                break;
            } else {

                try {
                    SystemUtils.LOG.info("Try " + tries + " did not work. Please wait 30s...");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tries > MAX_DOWNLOAD_TRIES && (entryFp == null || entryFp.getSize() != entry.contentLength)) {
                    SystemUtils.LOG.info("Skiping...");
                    break;
                }
                tries++;
            }
        }

        if (tries > MAX_DOWNLOAD_TRIES) {
            SystemUtils.LOG.warning("Resuming tries for file " + entry.fileName + " did not work.");
        } else {
            if (entryFp.getMd5Checksum().equalsIgnoreCase(entry.hexChecksum)) {
                SystemUtils.LOG.info("Filename: " + entry.fileName + " downloaded and checked " + entry.hexChecksum);
            } else {
                SystemUtils.LOG.severe("Filename: " + entry.fileName + " downloaded [INVALID CHECKSUM]");
            }
        }
    }

    public void getProduct(final String id, final Entry entry, final File outputFolder, final ProgressMonitor pm) throws IOException {

        int tries = 1;
        HTTPDownloader.EntryFileProperty entryFp = null;
        while ((entryFp == null || entryFp.getSize() != entry.contentLength)) {

            final String downloadURL = odataRoot+"Products('" + id + "')" + "/$value?";

            entryFp = downloader.getEntryFilePropertyFromUrlString(downloadURL,
                                                                   entry.fileName, entry.contentLength, entry.contentType,
                                                                   outputFolder,
                                                                   credentialInfo.getUser(), credentialInfo.getPassword(),
                                                                   pm);

            if ((entryFp != null && entryFp.getSize() == entry.contentLength) || pm.isCanceled()) {
                break;
            } else {

                try {
                    SystemUtils.LOG.info("Try " + tries + " did not work. Please wait 30s...");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if ((tries > MAX_DOWNLOAD_TRIES && (entryFp == null || entryFp.getSize() != entry.contentLength)) || pm.isCanceled()){
                    SystemUtils.LOG.info("Skiping...");
                    break;
                }
                tries++;
            }
        }

        if (pm.isCanceled()) {
            SystemUtils.LOG.info("OpenData: Download is cancelled");
            return;
        }

        if (tries > MAX_DOWNLOAD_TRIES) {
            SystemUtils.LOG.warning("Resuming tries for file " + entry.fileName + " did not work.");
            throw new IOException(entry.fileName);
        } else {
            if (entryFp.getMd5Checksum().equalsIgnoreCase(entry.hexChecksum)) {
                SystemUtils.LOG.info("Filename: " + entry.fileName + " downloaded and checked " + entry.hexChecksum);
            } else {
                SystemUtils.LOG.severe("Filename: " + entry.fileName + " downloaded [INVALID CHECKSUM]");
            }
        }
    }

    private Entry readEntry(final String serviceUri, final String contentType, final String entitySetName,
                            final String keyValue, final String params)
            throws IOException {
        try {
            final EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();

            final InputStream content = downloader.connect(serviceUri + entitySetName + "('" + keyValue + "')" + params,
                                                           contentType, HTTPDownloader.HTTP_METHOD_GET,
                                                           credentialInfo.getUser(), credentialInfo.getPassword());
            final ODataEntry oDataEntry = EntityProvider.readEntry(contentType, entityContainer.getEntitySet(entitySetName),
                                                                   content, EntityProviderReadProperties.init().build());

            return new Entry(oDataEntry);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Credentials.CredentialInfo getCredentialInfo() throws IOException {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(host);
        if (credentialInfo == null) {
            throw new IOException("Credentials for " + host + " not set");
        }
        return credentialInfo;
    }

    public static class Entry {
        public final String hexChecksum;
        public final Long contentLength;
        public final String fileName;
        public final String name;
        public final String contentType;
        public final GeoPos[] footprint;

        private static final String gmlCoordStart = "<gml:coordinates>";
        private static final String gmlCoordEnd = "</gml:coordinates>";

        public Entry(final ODataEntry oDataEntry) {
            final Map<String, Object> propMap = oDataEntry.getProperties();

            contentLength = Long.parseLong(propMap.get("ContentLength").toString());
            contentType = propMap.get("ContentType").toString();

            name = propMap.get("Name").toString();
            fileName = propMap.get("Name") + ".zip";

            final HashMap<String, Object> checksum = (HashMap<String, Object>) propMap.get("Checksum");
            hexChecksum = checksum.get("Value").toString();

            footprint = getCoordinates((String)propMap.get("ContentGeometry"));
        }

        private static GeoPos[] getCoordinates(final String geomStr) {
            String values = geomStr.substring(geomStr.indexOf(gmlCoordStart)+gmlCoordStart.length(),
                                                    geomStr.indexOf(gmlCoordEnd)).trim();
            values = values.replace(' ', ',');

            final List<GeoPos> geoPosList = new ArrayList<>();
            final StringTokenizer st = new StringTokenizer(values, ",");
            while (st.hasMoreTokens()) {
                final String latStr = st.nextToken();
                final String lonStr = st.nextToken();

                geoPosList.add(new GeoPos(Double.parseDouble(latStr), Double.parseDouble(lonStr)));
            }
            geoPosList.add(geoPosList.get(0));

            return geoPosList.toArray(new GeoPos[geoPosList.size()]);
        }
    }
}
