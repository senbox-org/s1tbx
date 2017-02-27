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
package org.esa.snap.engine_utilities.download.opensearch;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Credentials;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenSearch interface for searching
 */
public class OpenSearch {
    private final String host;
    private String searchURL;

    private final static int numRows = 100;

    public OpenSearch(final String host) {
        this.host = host;
    }

    public Result getPages(String searchURL) throws IOException {
        this.searchURL = searchURL;
        final Feed feed = connect(searchURL, null);
        if (feed == null) {
            return null;
        }
        final Result result = new Result(feed);

        SystemUtils.LOG.info("OpenSearch: " + result.totalResults + " total results on " + result.pages + " pages.");

        //dumpFeed(feed);

        return result;
    }

    public String[] getProductIDs(final Result result) {
        final List<String> uuidLst = new ArrayList<>();

        for (int item = 0; item < result.totalResults; item++) {
            try {
                final Feed feed = connect(searchURL, "&start=" + item + "&rows=" + numRows);

                SystemUtils.LOG.info("Paging results: \t " + item + "/" + result.totalResults + " - \t UUID collected: " + uuidLst.size() + "\r");

                dumpFeed(feed);

                final List<Entry> entries = feed.getEntries();
                for (Entry entry : entries) {
                    if (!uuidLst.contains(entry.getId().toString())) uuidLst.add(entry.getId().toString());
                }
            } catch (Exception e) {

            }
        }
        return uuidLst.toArray(new String[uuidLst.size()]);
    }

    private Feed connect(String searchURL, final String compl) throws IOException {
        Feed feed;
        try {
            final Abdera abdera = new Abdera();
            final AbderaClient client = new AbderaClient(abdera);
            client.usePreemptiveAuthentication(true);

            final Credentials.CredentialInfo credentialInfo = getCredentials();

            client.addCredentials(host, null, null,
                    new UsernamePasswordCredentials(credentialInfo.getUser(), credentialInfo.getPassword()));
            AbderaClient.registerTrustManager();

            if (compl != null) {
                searchURL = searchURL + " " + compl;
            }

            int end = searchURL.indexOf("search") + 9;
            String init = searchURL.substring(0, end);
            String last = searchURL.substring(end);

            try {
                searchURL = init + URLEncoder.encode(last, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IOException(e);
            }

            SystemUtils.LOG.info("OpenSearch: " + searchURL);

            ClientResponse resp = client.get(searchURL);

            if (resp.getType() == Response.ResponseType.SUCCESS) {
                Document<Feed> doc = resp.getDocument();
                feed = doc.getRoot();
            } else {
                throw new IOException("Error in OpenSearch query: " + resp.getType() + " [" + resp.getStatus() + "]");
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return feed;
    }

    private Credentials.CredentialInfo getCredentials() throws IOException {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(host);
        if (credentialInfo == null) {
            throw new IOException("Credentials for " + host + " not found.");
        }
        return credentialInfo;
    }

    private static void dumpFeed(final Feed feed) {
        final List<Entry> entries = feed.getEntries();
        int i = 1;
        for (Entry entry : entries) {
            System.out.println("Item " + i + " of " + entries.size());
            System.out.println(entry.getId().toString());
            System.out.println(entry.getSummary());
            System.out.println(entry.getTitle());

            List<Link> links = entry.getLinks();
            for (Link link : links) {
                System.out.println("link: " + link.toString());
            }

            List<QName> attrib = entry.getAttributes();
            for (QName qName : attrib) {
                System.out.println("Atrib: " + qName.toString());
            }
            ++i;
        }
    }

    public static class Result {
        public final int totalResults;
        public final int itemsPerPage;
        public final int pages;

        public Result(final Feed feed) {
            QName trQn = new QName("http://a9.com/-/spec/opensearch/1.1/", "totalResults");
            QName ippQn = new QName("http://a9.com/-/spec/opensearch/1.1/", "itemsPerPage");
            totalResults = Integer.parseInt(feed.getExtension(trQn).getText());
            itemsPerPage = Integer.parseInt(feed.getExtension(ippQn).getText());
            pages = totalResults / itemsPerPage;
        }
    }
}
