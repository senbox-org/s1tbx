/**
 * The core {@code binio} API.
 *
 * <p>{@code binio} is a low-level API used to read and write arbitrarily formetted binary files.
 * It imposes the following programming model:</p>
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
 * {@link com.bc.ceres.binio.util.TypeParser} class.</p>
 */
package com.bc.ceres.binio;