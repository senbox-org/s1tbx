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
package org.esa.s1tbx.io.ceos.alos;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


class AlosPalsarTrailerFile extends AlosPalsarLeaderFile {

    private final static String trailer_recordDefinitionFile = "trailer_file.xml";
    private final static Document trailerXML = BinaryDBReader.loadDefinitionFile(mission, trailer_recordDefinitionFile);

    public AlosPalsarTrailerFile(final ImageInputStream stream) throws IOException {
        super(stream, trailerXML);

    }
}
