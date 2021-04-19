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
package org.esa.s1tbx.cloud.opensearch;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenSearch interface for searching
 */
public class OpenSearch {

    private final AbderaClient client;
    private String searchURL;

    private final static int numRows = 100; // 100 is maximum allowed by SciHub
    private final static int TIMEOUT = 60000; // milliseconds

    public OpenSearch(final String host, final String userName, final String password) throws IOException {
        try {
            client = new AbderaClient(new Abdera());
            client.usePreemptiveAuthentication(true);
            client.setConnectionTimeout(TIMEOUT);
            client.setConnectionManagerTimeout(TIMEOUT);
            client.setSocketTimeout(TIMEOUT);
            client.setMaxConnectionsPerHost(2);

            client.addCredentials(host, null, null,
                                  new UsernamePasswordCredentials(userName, password));
            AbderaClient.registerTrustManager();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public PageResult getPages(String searchURL) throws IOException {
        this.searchURL = searchURL;
        ClientResponse response[] = new ClientResponse[1];
        final Feed feed = connect(searchURL, "&start=" + 0 + "&rows=" + numRows, response);
        if (feed == null) {
            response[0].getInputStream().close();
            return null;
        }

        //dumpFeed(feed);

        final PageResult result = new PageResult(feed);

        System.out.println("OpenSearch: " + result.totalResults + " total results on " + result.pages + " pages.");

        response[0].getInputStream().close();

        return result;
    }

    public SearchResult[] getSearchResults(final PageResult pageResult) throws Exception {
        final List<SearchResult> searchResults = new ArrayList<>();
        final ClientResponse response[] = new ClientResponse[1];
        for (int item = 0; item < pageResult.totalResults; item += numRows) {
            final Feed feed = connect(searchURL, "&start=" + item + "&rows=" + numRows, response);
            if (feed == null) {
                if(response[0] != null) {
                    response[0].getInputStream().close();
                }
                return searchResults.toArray(new SearchResult[0]);
            }

            //dumpFeed(feed);

            final List<Entry> entries = feed.getEntries();
            for (Entry entry : entries) {
                searchResults.add(new SearchResult(entry));
            }
        }
        if(response[0] != null) {
            response[0].getInputStream().close(); // close connection
        }
        return searchResults.toArray(new SearchResult[0]);
    }

    private Feed connect(String searchURL, final String compl, final ClientResponse[] response) throws IOException {

        System.out.println("OpenSearch: " + searchURL);

        int end = searchURL.indexOf("search") + 9;
        String init = searchURL.substring(0, end);
        String last = searchURL.substring(end);

        try {
            searchURL = init + URLEncoder.encode(last, "UTF-8") + compl;
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }

        final ClientResponse resp = client.get(searchURL);

        if (resp.getType() == Response.ResponseType.SUCCESS) {
            Document<Feed> doc = resp.getDocument();
            response[0] = resp;
            return doc.getRoot();
        } else {
            throw new IOException("Error in OpenSearch query: " + resp.getType() + " [" + resp.getStatus() + ']');
        }
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

    public static class SearchResult {
        public final String id;
        public final String title;
        public URL url;

        public SearchResult(final Entry entry) throws Exception {
            this.id = entry.getId().toASCIIString();
            this.title = entry.getTitle();

            final List<Link> linkList = entry.getLinks();
            for(Link link :linkList) {
                if (link.getRel() == null) {
                    this.url = link.getHref().toURL();
                    break;
                }
            }
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
}
