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

package org.esa.beam.framework.dataio;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @deprecated no replacement
 */
@Deprecated
public interface ProfileReaderPlugIn {

    /**
     * Creates a ProductReaderPlugIn for a given profile. The profile is given by the fully classified class name
     * (obtained by <code>.getClass().getName()</code>) of the profile.
     *
     * @param profileClassName the class name
     *
     * @return the ProductReaderPlugIn
     */
    ProductReaderPlugIn createProfileReaderPlugin(String profileClassName);
}
