/*
 * $Id: ProductReaderPlugIn.java,v 1.3 2006/09/29 06:53:30 marcop Exp $
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
package org.esa.beam.framework.dataio;

/**
 * The <code>ProductReaderPlugIn</code> interface is implemented by data product reader plug-ins.
 * <p/>
 * <p>XMLDecoder plug-ins are used to provide meta-information about a particular data format and to create instances of
 * the actual reader objects.
 * <p/>
 * <p> A plug-in can register itself in the <code>ProductIO</code> plug-in registry or it is automatically found during
 * a classpath scan.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see ProductWriterPlugIn
 */
public interface ProductReaderPlugIn extends ProductIOPlugIn {

    /**
     * Gets the qualification of the product reader to decode a given input object.
     *
     * @param input the input object
     * @return  the decode qualification
     */
    DecodeQualification getDecodeQualification(Object input);

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    Class[] getInputTypes();

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    ProductReader createReaderInstance();

}
