/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for mapping unit strings to CF compliant unit strings.
 *
 * @author Marco Peters
 */
class CfCompliantUnitMapper {

    private static final Map<String, String> unitMap = new HashMap<String, String>();

    static {
        unitMap.put("deg", "degree");
    }

    private CfCompliantUnitMapper() {
    }

    /**
     * Tries to find a CF compliant unit string for the given one. If none is found the original unit string is returned.
     *
     * @param unit The unit string to find a CF compliant unit string for.
     *
     * @return A CF compliant unit string. If none is found the original unit string is returned.
     */
    public static String tryFindUnitString(String unit) {
        if (unitMap.containsKey(unit)) {
            return unitMap.get(unit);
        }
        return unit;
    }
}
