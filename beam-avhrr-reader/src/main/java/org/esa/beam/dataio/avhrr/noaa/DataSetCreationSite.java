/*
 * $Id: DataSetCreationSite.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.avhrr.noaa;

import java.util.HashMap;
import java.util.Map;

class DataSetCreationSite {

	private static Map datasetCreationSites;
	
	public static boolean hasDatasetCreationSite(String dscsID) {
        if (datasetCreationSites == null) {
            initDatasetCreationSites();
        }
        return datasetCreationSites.containsKey(dscsID);
    }

    public static String getDatasetCreationSite(String dscsID) {
        if (datasetCreationSites == null) {
            initDatasetCreationSites();
        }
        final String dscs = (String) datasetCreationSites.get(dscsID);
        return dscs != null ? dscs : "Unknown";
    }

    private static void initDatasetCreationSites() {
        datasetCreationSites = new HashMap();
        datasetCreationSites.put("CMS", "Centre de Meteorologie Spatiale (France)");
        datasetCreationSites.put("DSS", "Dundee Satellite Receiving Station (UK)");
        datasetCreationSites.put("NSS", "National Environmental Satellite, Data and Information Service (USA)");
        datasetCreationSites.put("UKM", "United Kingdom Meteorological Office (UK)");
    }
    
    private DataSetCreationSite() {
    }
}
