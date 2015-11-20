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
 * Provides special support for raster data operations that transforms each source pixel
 * into a corresponding target pixel at the same position. It is much easier to implement your operator if it inherits
 * from {@link org.esa.snap.core.gpf.pointop.SampleOperator} or {@link org.esa.snap.core.gpf.pointop.PixelOperator}
 * rather than {@link org.esa.snap.core.gpf.Operator} if you don't perform any geometric transformation
 * and don't need any source pixel neighborhood information.
 *
 * @author Norman Fomferra
 */
package org.esa.snap.core.gpf.pointop;