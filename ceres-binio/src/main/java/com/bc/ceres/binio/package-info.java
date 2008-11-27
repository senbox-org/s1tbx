/**
 * The core {@code binio} API.
 *
 * <p>{@code binio} is a low-level API used to read and write arbitrarily formetted binary files.
 * It imposes the following programming model:</p>
 *
 * <pre>
 *     // Static format declaration:
 *     {@link CompoundType} type = {@link TypeBuilder}.COMP("dataset", ...);
 *     {@link DataFormat} format = new Format(type);
 *
 *     // Reading/writing a file using the given format:
 *     RandomAccessFile raf = new RandomAccessFile(file, "rw");
 *     {@link IOHandler} handler = new RandomAccessFileIOHandler(raf);
 *     {@link com.bc.ceres.binio.internal.DataContextImpl} context = format.createContext(file);
 *     {@link CompoundData} compoundData = context.getData();
 *     // Here: Invoke methods on {@code compoundData} ...
 *     context.dispose();
 * </pre>
 *
 * <p>Note that you can use the {@link com.bc.ceres.binio.util.TypeBuilder} class to easily construct
 * types in Java. Types can also be read from external plain text files using the
 * {@link com.bc.ceres.binio.util.TypeParser} class.</p>
 */
package com.bc.ceres.binio;