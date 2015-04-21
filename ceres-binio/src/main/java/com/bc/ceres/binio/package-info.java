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

/**
 * The core {@code binio} API.
 *
 * <p>{@code binio} is a low-level API used to read and write arbitrarily formetted binary files.
 * It imposes the following programming model:
 *
 * <pre>
 *     // Static format declaration:
 *     {@link CompoundType} type = {@link TypeBuilder}.COMPOUND("dataset", ...);
 *     {@link DataFormat} format = new Format(type);
 *
 *     // Reading/writing a file using the given format:
 *     {@link com.bc.ceres.binio.internal.DataContextImpl} context = format.createContext(file);
 *     {@link CompoundData} compoundData = context.getData();
 *     // Here: Invoke methods on {@code compoundData} ...
 *     context.dispose();
 * </pre>
 *
 * <p>The {@link TypeBuilder} class to easily build complex types.
 * Types can also be read from external plain text files using the
 * {@link com.bc.ceres.binio.util.TypeParser} class.
 */
package com.bc.ceres.binio;