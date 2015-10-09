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

package org.esa.snap.core.dataio.dimap.spi;

import org.jdom.Element;

/**
 * The service provider interface (SPI) for <code>DimapPersistable</code>s.
 *
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i>
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public interface DimapPersistableSpi {

    boolean canDecode(Element element);

    boolean canPersist(Object object);

    DimapPersistable createPersistable();

}
