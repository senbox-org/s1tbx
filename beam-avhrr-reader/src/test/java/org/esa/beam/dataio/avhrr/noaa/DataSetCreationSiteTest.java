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

package org.esa.beam.dataio.avhrr.noaa;

import junit.framework.TestCase;

public class DataSetCreationSiteTest extends TestCase {

	// Important for file format detection
	public void testDatasetCreationSites() {
		assertTrue(DataSetCreationSite.hasDatasetCreationSite("CMS"));
		assertTrue(DataSetCreationSite.hasDatasetCreationSite("DSS"));
		assertTrue(DataSetCreationSite.hasDatasetCreationSite("NSS"));
		assertTrue(DataSetCreationSite.hasDatasetCreationSite("UKM"));
		assertFalse(DataSetCreationSite.hasDatasetCreationSite("XYZ"));

		assertNotNull(DataSetCreationSite.getDatasetCreationSite("CMS"));
		assertNotNull(DataSetCreationSite.getDatasetCreationSite("DSS"));
		assertNotNull(DataSetCreationSite.getDatasetCreationSite("NSS"));
		assertNotNull(DataSetCreationSite.getDatasetCreationSite("UKM"));
		assertEquals("Unknown", DataSetCreationSite.getDatasetCreationSite("XYZ"));
	}
}
