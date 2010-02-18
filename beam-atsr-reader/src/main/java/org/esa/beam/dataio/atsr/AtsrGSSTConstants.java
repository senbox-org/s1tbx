/*
 * $Id: AtsrGSSTConstants.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.atsr;

/**
 * This interface defines frequently used constants for ERS GSST products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public interface AtsrGSSTConstants {

    String SST_UNIT = "K";
    float SST_FACTOR = 0.01f;
    String NADIR_SST_NAME = "nadir_view_sst";
    String NADIR_SST_DESCRIPTION = "Nadir-only sea-surface temperature"; /*I18N*/
    String DUAL_SST_NAME = "dual_view_sst";
    String DUAL_SST_DESCRIPTION = "Dual-view sea-surface temperature"; /*I18N*/

    String SST_CONFIDENCE_NAME = "confid_flags";
    String SST_CONFIDENCE_DESCRIPTION = "Sea-surface temperature confidence words"; /*I18N*/

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

    // flags codings and names
    String CONFIDENCE_FLAGS_NAME = "confid_flags";

    String NADIR_SST_VALID_FLAG_NAME = "NADIR_SST_VALID";
    int NADIR_SST_VALID_FLAG_MASK = 0x1;
    String NADIR_SST_VALID_FLAG_DESCRIPTION = "Nadir-only sea-surface temperature is valid (if not set, pixel contains nadir-view 11 um brightness temperature"; /*I18N*/

    String NADIR_SST_37_FLAG_NAME = "NADIR_SST_37_INCLUDED";
    int NADIR_SST_37_FLAG_MASK = 0x2;
    String NADIR_SST_37_FLAG_DESCRIPTION = "Nadir-only sea-surface temperature retrieval includes 3.7 um channel (if not set, retrieval includes 12 um and 11 um only)"; /*I18N*/

    String DUAL_SST_VALID_FLAG_NAME = "DUAL_SST_VALID";
    int DUAL_SST_VALID_FLAG_MASK = 0x4;
    String DUAL_SST_VALID_FLAG_DESCRIPTION = "Dual-view sea-surface temperature is valid (if not set, pixel contains nadir-view 11 um brightness temperature"; /*I18N*/

    String DUAL_SST_37_FLAG_NAME = "DUAL_SST_37_INCLUDED";
    int DUAL_SST_37_FLAG_MASK = 0x8;
    String DUAL_SST_37_FLAG_DESCRIPTION = "Dual-view sea-surface temperature retrieval includes 3.7 um channel (if not set, retrieval includes 12 um and 11 um only)"; /*I18N*/

    String LAND_FLAG_NAME = "LAND";
    int LAND_FLAG_MASK = 0x10;
    String LAND_FLAG_DESCRIPTION = "Pixel is over land"; /*I18N*/

    String NADIR_CLOUDY_FLAG_NAME = "NADIR_CLOUDY";
    int NADIR_CLOUDY_FLAG_MASK = 0x20;
    String NADIR_CLOUDY_FLAG_DESCRIPTION = "Nadir-view pixel is cloudy"; /*I18N*/

    String NADIR_BLANKING_FLAG_NAME = "NADIR_BLANKING";
    int NADIR_BLANKING_FLAG_MASK = 0x40;
    String NADIR_BLANKING_FLAG_DESCRIPTION = "Nadir-view pixel has blanking-pulse"; /*I18N*/

    String NADIR_COSMETIC_FLAG_NAME = "NADIR_COSMETIC";
    int NADIR_COSMETIC_FLAG_MASK = 0x80;
    String NADIR_COSMETIC_FLAG_DESCRIPTION = "Nadir-view pixel is cosmetic (nearest-neighbour fill)"; /*I18N*/

    String FORWARD_CLOUDY_FLAG_NAME = "FWARD_CLOUDY";
    int FORWARD_CLOUDY_FLAG_MASK = 0x100;
    String FORWARD_CLOUDY_FLAG_DESCRIPTION = "Forward-view pixel is cloudy"; /*I18N*/

    String FORWARD_BLANKING_FLAG_NAME = "FWARD_BLANKING";
    int FORWARD_BLANKING_FLAG_MASK = 0x200;
    String FORWARD_BLANKING_FLAG_DESCRIPTION = "Forward-view pixel has blanking-pulse"; /*I18N*/

    String FORWARD_COSMETIC_FLAG_NAME = "FWARD_COSMETIC";
    int FORWARD_COSMETIC_FLAG_MASK = 0x400;
    String FORWARD_COSMETIC_FLAG_DESCRIPTION = "Forward-view pixel is cosmetic (nearest-neighbour fill)"; /*I18N*/
}
