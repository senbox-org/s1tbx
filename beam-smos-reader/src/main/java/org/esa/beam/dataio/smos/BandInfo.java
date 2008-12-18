/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

/**
 * Band info.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class BandInfo {

    final String name;
    final String unit;

    final double scaleOffset;
    final double scaleFactor;
    final Number noDataValue;
    final double min;
    final double max;
    final String description;

    BandInfo(String name) {
        this(name, "", 0.0, 1.0, -999.0, 0.0, 1000.0, "");
    }

    BandInfo(String name, String unit, double scaleOffset, double scaleFactor, Number noDataValue, double min,
             double max, String description) {
        this.name = name;
        this.unit = unit;
        this.scaleOffset = scaleOffset;
        this.scaleFactor = scaleFactor;
        this.noDataValue = noDataValue;
        this.min = min;
        this.max = max;
        this.description = description;
    }
}
