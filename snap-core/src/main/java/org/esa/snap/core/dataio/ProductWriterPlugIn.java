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
package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.Product;

/**
 * The <code>ProductWriterPlugIn</code> interface is implemented by data product writer plug-ins.
 * <p>XMLCoder plug-ins are used to provide meta-information about a particular data format and to create instances of
 * the actual writer objects.
 * <p> A plug-in can register itself in the <code>ProductIO</code> plug-in registry or it is automatically found during
 * a classpath scan.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see ProductReaderPlugIn
 */
public interface ProductWriterPlugIn extends ProductIOPlugIn {

    /**
     * Gets the encode qualification of this product writer plugin w.r.t. the given product.
     *
     * @param product The product.
     * @return The encode qualification.
     * @since SNAP 2.0
     */
    EncodeQualification getEncodeQualification(Product product);

    /**
     * Returns an array containing the classes that represent valid output types for this writer.
     * <p> Intances of the classes returned in this array are valid objects for the <code>setOutput</code> method of the
     * <code>ProductWriter</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see ProductWriter#writeProductNodes
     */
    Class[] getOutputTypes();

    /**
     * Creates an instance of the actual product writer class. This method should never return <code>null</code>.
     *
     * @return a new writer instance, never <code>null</code>
     */
    ProductWriter createWriterInstance();

}
