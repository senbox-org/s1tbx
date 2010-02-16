/*
 * $Id: DescriptorLoadedAsServiceTest.java,v 1.2 2006/09/21 07:43:28 marcop Exp $
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
package org.esa.beam.dataio.getasse30;

import junit.framework.TestCase;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class DescriptorLoadedAsServiceTest extends TestCase {

    public void testDescriptorIsLoaded() {
        ElevationModelRegistry registry = ElevationModelRegistry.getInstance();
        ElevationModelDescriptor descriptor = registry.getDescriptor("GETASSE30");

        assertNotNull(descriptor);

    }

}
