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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Credentials;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenSearch interface for searching
 */
public class OpenSearch {
    private final String host;
    private final AbderaClient client;
    private String searchURL;

    private final static int numRows = 100;

    public OpenSearch(final String host, final Credentials.CredentialInfo credentialInfo) throws IOException {
        this.host = host;

        try {
            client = new AbderaClient(new Abdera());
            client.usePreemptiveAuthentication(true);

            client.addCredentials(host, null, null,
                                  new UsernamePasswordCredentials(credentialInfo.getUser(), credentialInfo.getPassword()));
            AbderaClient.registerTrustManager();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public PageResult getPages(String searchURL) throws IOException {
        this.searchURL = searchURL;
        final Feed feed = connect(searchURL, "&start=" + 0 + "&rows=" + numRows);
        if (feed == null) {
            return null;
        }
        final PageResult result = new PageResult(feed);

        SystemUtils.LOG.info("OpenSearch: " + result.totalResults + " total results on " + result.pages + " pages.");

        //dumpFeed(feed);

        return result;
    }

    public ProductResult[] getProductResults(final PageResult result) {
        final List<ProductResult> productResultList = new ArrayList<>();

        for (int item = 0; item < result.totalResults; item += numRows) {
            try {
                final Feed feed = connect(searchURL, "&start=" + item + "&rows=" + numRows);

                //SystemUtils.LOG.info("Paging results: \t " + (item+1) + '/' + result.pages);
                //dumpFeed(feed);

                final List<Entry> entries = feed.getEntries();
                for (Entry entry : entries) {
                    productResultList.add(new ProductResult(entry));
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe("Error retrieving product results " + e.getMessage());
            }
        }
        return productResultList.toArray(new ProductResult[productResultList.size()]);
    }

    private Feed connect(String searchURL, final String compl) throws IOException {
        Feed feed;
        //try {


            if (compl != null) {
                searchURL = searchURL + ' ' + compl;
            }

            SystemUtils.LOG.info("OpenSearch: " + searchURL);

            int end = searchURL.indexOf("search") + 9;
            String init = searchURL.substring(0, end);
            String last = searchURL.substring(end);

            try {
                searchURL = init + URLEncoder.encode(last, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IOException(e);
            }

            ClientResponse resp = client.get(searchURL);

            if (resp.getType() == Response.ResponseType.SUCCESS) {
                Document<Feed> doc = resp.getDocument();
                feed = doc.getRoot();
            } else {
                throw new IOException("Error in OpenSearch query: " + resp.getType() + " [" + resp.getStatus() + ']');
            }

        return feed;
    }

    private static void dumpFeed(final Feed feed) {
        final List<Entry> entries = feed.getEntries();
        int i = 1;
        for (Entry entry : entries) {
            System.out.println("Item " + i + " of " + entries.size());
            System.out.println(entry.getId().toString());
            System.out.println(entry.getSummary());
            System.out.println(entry.getTitle());

            final List<Link> links = entry.getLinks();
            for (Link link : links) {
                System.out.println("link: " + link.toString());
            }

            final List<QName> attrib = entry.getAttributes();
            for (QName qName : attrib) {
                System.out.println("Atrib: " + qName.toString());
            }
            ++i;
        }
    }

    public static class PageResult {
        public final int totalResults;
        public final int itemsPerPage;
        public final int pages;

        private static final QName trQn = new QName("http://a9.com/-/spec/opensearch/1.1/", "totalResults");
        private static final QName ippQn = new QName("http://a9.com/-/spec/opensearch/1.1/", "itemsPerPage");

        public PageResult(final Feed feed) {
            totalResults = Integer.parseInt(feed.getExtension(trQn).getText());
            itemsPerPage = Integer.parseInt(feed.getExtension(ippQn).getText());
            pages = (totalResults / itemsPerPage) + 1;
        }
    }

    public static class ProductResult {
        public final String id;
        public final String name;
        public final String size;
        public final String mode;
        public final String mission;
        public final ProductData.UTC utc;
        public String productLink;
        public String quicklookLink;

        private static final String SIZE = "Size:";
        private static final String SATELLITE = "Satellite:";
        private static final String DATE = "Date:";
        private static final String MODE = "Mode:";
        public final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyy-MM-dd HH:mm:ss");

        public ProductResult(final Entry entry) {
            this.id = (entry.getId().toString());
            this.name = entry.getTitle();

            final String summary = entry.getSummary();

            this.mission = getMission(summary);
            this.utc = AbstractMetadata.parseUTC(getDate(summary), dateFormat);
            this.size = getSize(summary);
            this.mode = getMode(summary);

            final List<Link> links = entry.getLinks();
            for (Link link : links) {

                if(link.getRel() == null) {
                    productLink = link.getHref().toString();
                } else if(link.getRel().equals("icon")) {
                    quicklookLink = link.getHref().toString();
                }
            }
        }

        private static String getDate(final String text) {
            int start = text.indexOf(DATE);
            if(start >= 0) {
                int end = text.indexOf(',', start);
                return text.substring(start + DATE.length(), end < 0 ? text.length() : end).trim().replace("T", " ").replace("Z", "");
            }
            return "";
        }

        private static String getMission(final String text) {
            int start = text.indexOf(SATELLITE);
            if(start >= 0) {
                int end = text.indexOf(',', start);
                return text.substring(start + SATELLITE.length(), end < 0 ? text.length() : end).trim();
            }
            return "";
        }

        private static String getSize(final String text) {
            int start = text.indexOf(SIZE);
            if(start >= 0) {
                int end = text.indexOf(',', start);
                return text.substring(start + SIZE.length(), end < 0 ? text.length() : end).trim();
            }
            return "";
        }

        private static String getMode(final String text) {
            int start = text.indexOf(MODE);
            if(start >= 0) {
                int end = text.indexOf(',', start);
                return text.substring(start + MODE.length(), end < 0 ? text.length() : end).trim();
            }
            return "";
        }
    }
}
