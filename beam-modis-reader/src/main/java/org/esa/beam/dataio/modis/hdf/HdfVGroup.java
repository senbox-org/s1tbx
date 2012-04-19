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
package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.dataio.modis.hdf.lib.HDF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HdfVGroup {

    private int _id;
    private String _name;
    private String _class;

    /**
     * Retrieves a vector of all HDF vgroups contained in the file.
     *
     * @param fileId the file identifier as returned by Hopen().
     * @return a vector of groups
     */
    public static HdfVGroup[] getGroups(int fileId) throws IOException {
        final IHDF ihdf = HDF.getWrap();
        if (!ihdf.Vstart(fileId)) {
            throw new IOException("Unable to access HDF file.");
        }

        final List<HdfVGroup> groupList = new ArrayList<HdfVGroup>();

        // get the total number of lone vgroups
        final int numGroups = ihdf.Vlone(fileId, new int[1], 0);
        if (numGroups > 0) {
            final int[] refArray = new int[numGroups];
            ihdf.Vlone(fileId, refArray, refArray.length);

            // now loop over groups
            for (int n = 0; n < refArray.length; n++) {
                final int groupId = ihdf.Vattach(fileId, refArray[n], "r");

                if (groupId == HDFConstants.FAIL) {
                    ihdf.Vdetach(groupId);
                    continue;
                }

                String[] s = {""};
                ihdf.Vgetname(groupId, s);
                final String groupName = s[0].trim();

                ihdf.Vgetclass(groupId, s);
                final String groupClass = s[0].trim();

                groupList.add(new HdfVGroup(groupId, groupName, groupClass));
            }
        }

        ihdf.Vend(fileId);

        return groupList.toArray(new HdfVGroup[groupList.size()]);
    }

    /**
     * Retrieves the identifier of the group.
     *
     * @return the groupId
     */
    public int getId() {
        return _id;
    }

    /**
     * Retrieves the name of the group
     *
     * @return the group name
     */
    public String getName() {
        return _name;
    }

    /**
     * Retrieves the hdf class of the group
     *
     * @return the class
     */
    public String getGrpClass() {
        return _class;
    }

    /**
     * Constructs a group object with given identifier, name and class
     *
     * @param groupId
     * @param groupName
     * @param groupClass
     */
    public HdfVGroup(int groupId, String groupName, String groupClass) {
        _id = groupId;
        _name = groupName;
        _class = groupClass;
    }
}
