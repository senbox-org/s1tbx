/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/**
 * Contains the SNAP product I/O framework.
 * The product I/O framework is used to dynamically integrate new product readers and writers
 * for different data formats into SNAP. A product reader or writer
 * plug-ins implements the {@link org.esa.snap.core.dataio.ProductReaderPlugIn} resp. {@link org.esa.snap.core.dataio.ProductWriterPlugIn} interface.
 * All plug-ins must be registered using the {@link org.esa.snap.core.dataio.ProductIOPlugInManager} before they can
 * be be accessed through the {@link org.esa.snap.core.dataio.ProductIO} utility class.
 *
 * @see org.esa.snap.core.dataio.ProductReaderPlugIn
 * @see org.esa.snap.core.dataio.ProductWriterPlugIn
 * @see org.esa.snap.core.dataio.ProductIOPlugInManager
 * @see org.esa.snap.core.dataio.ProductIO
 */
package org.esa.snap.core.dataio;
