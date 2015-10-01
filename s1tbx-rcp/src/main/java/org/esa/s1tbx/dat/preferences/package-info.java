/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
@OptionsPanelController.ContainerRegistration(
        id = "S1TBX",
        categoryName = "#LBL_S1TBXOptionsCategory_Name",
        iconBase = "org/esa/s1tbx/dat/icons/s1_32x.png",
        keywords = "#LBL_S1TBXOptionsCategory_Keywords",
        keywordsCategory = "S1TBX",
        position = 1000
)
@NbBundle.Messages(value = {
    "LBL_S1TBXOptionsCategory_Name=S1TBX",
    "LBL_S1TBXOptionsCategory_Keywords=s1tbx,sar"
})
package org.esa.s1tbx.dat.preferences;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;