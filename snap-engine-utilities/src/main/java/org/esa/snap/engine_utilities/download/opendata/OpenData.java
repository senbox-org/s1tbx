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

import org.esa.snap.engine_utilities.datamodel.Credentials;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenData interface for downloading
 */
public class OpenData {

    private final String host;

    private static final String APPLICATION_XML = "application/xml";
    private static final int MAX_DOWNLOAD_TRIES = 5;

    public OpenData(final String host) {
        this.host = host;
    }

    public void getProductByID(final String id, final String odataMetaLink, final String odataRoot, final String outputFolder) throws IOException {

        final Credentials.CredentialInfo credentialInfo = getCredentials();
        final HTTPDownloader downloader = new HTTPDownloader();

        HTTPDownloader.EntryFileProperty entryFp = null;
        String hexChecksum;
        Long contentLength;
        String fileName;
        String contentType;

        try {
            final InputStream content = downloader.connect(odataMetaLink, APPLICATION_XML, HTTPDownloader.HTTP_METHOD_GET,
                    credentialInfo.getUser(), credentialInfo.getPassword());

            final Edm edm = EntityProvider.readMetadata(content, false);
            if (content != null) {
                content.close();
            }

            ODataEntry entry = readEntry(downloader, edm, odataRoot, APPLICATION_XML, "Products", id, "?platformname=Sentinel-1",
                    credentialInfo.getUser(), credentialInfo.getPassword());

            final Map<String, Object> propMap = entry.getProperties();

            contentLength = Long.parseLong(propMap.get("ContentLength").toString());
            contentType = propMap.get("ContentType").toString();

            fileName = propMap.get("Name") + ".zip";
            HashMap<String, Object> checksum = (HashMap<String, Object>) propMap.get("Checksum");
            hexChecksum = checksum.get("Value").toString();

            SystemUtils.LOG.info("Id: " + id);
            SystemUtils.LOG.info("Checksum: " + hexChecksum);
            SystemUtils.LOG.info("Filename: " + fileName);
            SystemUtils.LOG.info("ContentLength: " + contentLength);
            SystemUtils.LOG.info("ContentType: " + contentType);

        } catch (Exception e) {
            throw new IOException(e);
        }

        int tries = 1;
        while ((entryFp == null || entryFp.getSize() != contentLength)) {

            entryFp = downloader.getEntryFilePropertyFromUrlString("https://scihub.copernicus.eu/dhus/odata/v1/Products('"+id+"')/$value?platformname=Sentinel-2",
                    fileName, contentLength, contentType, outputFolder, credentialInfo.getUser(), credentialInfo.getPassword());

            if (entryFp != null && entryFp.getSize() == contentLength) {
                break;
            } else {

                try {
                    SystemUtils.LOG.info("Try " + tries + " did not work. Please wait 30s...");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tries > MAX_DOWNLOAD_TRIES && (entryFp == null || entryFp.getSize() != contentLength)) {
                    SystemUtils.LOG.info("Skiping...");
                    break;
                }
                tries++;
            }
        }

        if (tries > MAX_DOWNLOAD_TRIES) {
            SystemUtils.LOG.warning("Resuming tries for file " + fileName + " did not work.");
        } else {
            if (entryFp.getMd5Checksum().equalsIgnoreCase(hexChecksum)) {
                SystemUtils.LOG.info("Filename: " + fileName + " downloaded and checked " + hexChecksum);
            } else {
                SystemUtils.LOG.severe("Filename: " + fileName + " downloaded [INVALID CHECKSUM]");
            }
        }
    }

    private ODataEntry readEntry(final HTTPDownloader downloader, final Edm edm, final String serviceUri, final String contentType,
                                 final String entitySetName, final String keyValue, final String params,
                                 final String user, final String password) throws IOException, ODataException {

        final EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();

        InputStream content = downloader.connect(serviceUri + entitySetName + "('" + keyValue + "')" + params, contentType,
                HTTPDownloader.HTTP_METHOD_GET, user, password);
        return EntityProvider.readEntry(contentType, entityContainer.getEntitySet(entitySetName), content, EntityProviderReadProperties.init().build());
    }

    private Credentials.CredentialInfo getCredentials() throws IOException {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(host);
        if (credentialInfo == null) {
            throw new IOException("Credentials for " + host + " not found.");
        }
        return credentialInfo;
    }
}
