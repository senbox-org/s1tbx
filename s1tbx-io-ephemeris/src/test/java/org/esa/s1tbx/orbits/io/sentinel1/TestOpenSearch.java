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
package org.esa.s1tbx.orbits.io.sentinel1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestOpenSearch {

    private static final String query = "https://scihub.copernicus.eu/gnss/search?q=producttype:AUX_POEORB AND ingestiondate:[2021-02-10T10:00:000Z TO 2021-02-15T10:00:000Z] AND platformname:Sentinel-1";
    private final String COPERNICUS_HOST = "https://scihub.copernicus.eu";

    @Test
    public void testConnect() throws Exception  {
        final OpenSearch openSearch = new OpenSearch(COPERNICUS_HOST, "gnssguest","gnssguest");

        final OpenSearch.PageResult pageResult = openSearch.getPages(query);

        OpenSearch.SearchResult[] searchResults = openSearch.getSearchResults(pageResult);
        assertEquals(10, searchResults.length);


    }
}
