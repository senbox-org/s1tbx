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
