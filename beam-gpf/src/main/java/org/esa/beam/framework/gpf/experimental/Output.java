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

package org.esa.beam.framework.gpf.experimental;

/**
 * This is a marker interface which can be used by implementations of {@link org.esa.beam.framework.gpf.Operator} class
 * in order to indicate that the {@code Operator} marked by this interface takes care of the output itself and thus,
 * the framework shall not consider it as an operator that produces raster data to be written to a product file.
 * <p/>
 * <i>Important Note: This class is not part of the official API, we may remove or rename it later.</i>
 *
 * @author Marco Peters
 * @since BEAM 4.9
 */
public interface Output {
}
