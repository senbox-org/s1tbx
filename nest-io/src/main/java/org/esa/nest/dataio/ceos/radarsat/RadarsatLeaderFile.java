/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.radarsat;

import org.esa.nest.dataio.ceos.CEOSLeaderFile;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


class RadarsatLeaderFile extends CEOSLeaderFile {

    private final static String mission = "radarsat";
    private final static String leader_recordDefinitionFile = "leader_file.xml";

    public RadarsatLeaderFile(final ImageInputStream stream) throws IOException {
        super(stream, mission, leader_recordDefinitionFile);

    }

}