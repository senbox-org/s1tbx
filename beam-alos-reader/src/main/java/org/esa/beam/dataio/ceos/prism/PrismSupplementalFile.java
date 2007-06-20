/*
 * $Id: PrismSupplementalFile.java,v 1.1 2006/09/13 09:12:35 marcop Exp $
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
package org.esa.beam.dataio.ceos.prism;

import org.esa.beam.dataio.ceos.CeosFileReader;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

class PrismSupplementalFile {

    private CeosFileReader _ceosReader;

    public PrismSupplementalFile(final ImageInputStream supplementalStream) {
        _ceosReader = new CeosFileReader(supplementalStream);
    }

    public void close() throws IOException {
        _ceosReader.close();
    }
}
