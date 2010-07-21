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
package org.esa.beam.dataio.atsr;

/**
 * This interface defines frequently used constants for ERS GBT products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public interface AtsrGBTConstants {

    String BRIGHTNESS_TEMPERATURE_UNIT = "K";
    float BRIGHTNESS_TEMPERATURE_FACTOR = 0.01f;
    String NADIR_1200_BT_NAME = "btemp_nadir_1200";
    String NADIR_1200_BT_DESCRIPTION = "Nadir-view 12.0um brightness temperature image"; /*I18N*/
    String NADIR_1100_BT_NAME = "btemp_nadir_1100";
    String NADIR_1100_BT_DESCRIPTION = "Nadir-view 11.0um brightness temperature image"; /*I18N*/
    String NADIR_370_BT_NAME = "btemp_nadir_370";
    String NADIR_370_BT_DESCRIPTION = "Nadir-view 3.7um brightness temperature image"; /*I18N*/

    String FORWARD_1200_BT_NAME = "btemp_fward_1200";
    String FORWARD_1200_BT_DESCRIPTION = "Forward-view 12.0um brightness temperature image"; /*I18N*/
    String FORWARD_1100_BT_NAME = "btemp_fward_1100";
    String FORWARD_1100_BT_DESCRIPTION = "Forward-view 11.0um brightness temperature image"; /*I18N*/
    String FORWARD_370_BT_NAME = "btemp_fward_370";
    String FORWARD_370_BT_DESCRIPTION = "Forward-view 3.7um brightness temperature image"; /*I18N*/

    String REFLECTANCE_UNIT = "%";
    float REFLECTANCE_FACTOR = 0.01f;
    String NADIR_1600_REF_NAME = "reflec_nadir_1600";
    String NADIR_1600_REF_DESCRIPTION = "Nadir-view 1.6um reflectance image"; /*I18N*/
    String NADIR_870_REF_NAME = "reflec_nadir_870";
    String NADIR_870_REF_DESCRIPTION = "Nadir-view 0.87um reflectance image"; /*I18N*/
    String NADIR_650_REF_NAME = "reflec_nadir_650";
    String NADIR_650_REF_DESCRIPTION = "Nadir-view 0.65um reflectance image"; /*I18N*/
    String NADIR_550_REF_NAME = "reflec_nadir_550";
    String NADIR_550_REF_DESCRIPTION = "Nadir-view 0.55um reflectance image"; /*I18N*/

    String FORWARD_1600_REF_NAME = "reflec_fward_1600";
    String FORWARD_1600_REF_DESCRIPTION = "Forward-view 1.6um reflectance image"; /*I18N*/
    String FORWARD_870_REF_NAME = "reflec_fward_870";
    String FORWARD_870_REF_DESCRIPTION = "Forward-view 0.87um reflectance image"; /*I18N*/
    String FORWARD_650_REF_NAME = "reflec_fward_650";
    String FORWARD_650_REF_DESCRIPTION = "Forward-view 0.65um reflectance image"; /*I18N*/
    String FORWARD_550_REF_NAME = "reflec_fward_550";
    String FORWARD_550_REF_DESCRIPTION = "Forward-view 0.55um reflectance image"; /*I18N*/

    String COORDINATE_OFFSET_UNIT = "km";
    float COORDINATE_OFFSET_FACTOR = 1.f / 256.f;
    String NADIR_X_OFFS_NAME = "x_offs_nadir";
    String NADIR_X_OFFS_DESCRIPTION = "X coordinate offsets (across-track) of nadir view pixels"; /*I18N*/
    String NADIR_Y_OFFS_NAME = "y_offs_nadir";
    String NADIR_Y_OFFS_DESCRIPTION = "Y coordinate offsets (along-track) of nadir view pixels"; /*I18N*/

    String FORWARD_X_OFFS_NAME = "x_offs_fward";
    String FORWARD_X_OFFS_DESCRIPTION = "X coordinate offsets (across-track) of forward view pixels"; /*I18N*/
    String FORWARD_Y_OFFS_NAME = "y_offs_fward";
    String FORWARD_Y_OFFS_DESCRIPTION = "Y coordinate offsets (along-track) of forward view pixels"; /*I18N*/
}
