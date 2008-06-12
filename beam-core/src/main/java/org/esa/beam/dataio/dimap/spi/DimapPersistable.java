/*
 * $Id: DimapPersistable.java,v 1.2 2007/03/22 16:56:58 marcop Exp $
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
package org.esa.beam.dataio.dimap.spi;

import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;

/**
 * Interface to implemented by clients who know how to read objects from
 * and write object to BEAM-DIMAP XML.
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 * 
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public interface DimapPersistable {
    Object createObjectFromXml(Element element, Product product);
    Element createXmlFromObject(Object object);
}
