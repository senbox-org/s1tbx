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
package org.esa.beam.processor.binning.store;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
final class QuadTreeLeaf implements QuadTreeElement {

    private final int _xOff;
    private final int _yOff;
    private final int _width;
    private final String _layerIndex;
    private final int _numFloats;
    private final int _numVariables;
    private boolean _hasBuffer;
    private float[] _buffer;
    private final QuadTreeFile _qtFile;
    private File _leafFile;

    /**
     * Constructs the object with given parameters.
     *
     * @param offsetX    the x coordinate division line index
     * @param offsetY    the y coordinate division line index
     * @param width      width of this QuadTreeNode
     * @param height     the height of this QuadTreeNode
     * @param qtFile     the QuadTreeFile containing this element
     * @param layerIndex the index of the layer where this element is attached to the tree
     */
    public QuadTreeLeaf(int offsetX, int offsetY, int width, int height,
                        QuadTreeFile qtFile, String layerIndex) {
//        System.out.println("new leaf: x: " + offsetX + " y: " + offsetY + " w: " + width + " h: " + height);
        _xOff = offsetX;
        _yOff = offsetY;
        _width = width;
        _layerIndex = layerIndex;
        _hasBuffer = false;
        _qtFile = qtFile;
        _numVariables = _qtFile.getNumberOfVariables();
        _numFloats = _width * height * _numVariables;
    }

    /**
     * Creates the disk file connected to this leaf.
     */
    public final void create() throws IOException {
        createFile();
    }

    /**
     * Loads the disk file connected to this leaf.
     *
     * @param fileNames a vector of available file names
     */
    public final void load(Vector fileNames) throws IOException {
        _leafFile = _qtFile.createLeafFile(_layerIndex);

        String fileName = _leafFile.getName();
        boolean bRemoved = fileNames.removeElement(fileName.substring(0, fileName.lastIndexOf('.')));
        if (bRemoved) {
            // @todo 1 nf/nf - what then ???
        }
    }

    /**
     * Reads data at the given location.
     *
     * @param rowcol a Point designating the read location.
     * @param data   the array to be filled
     */
    public final void read(Point rowcol, float[] data) throws IOException {
        if (!_hasBuffer) {
            getAndReadBuffer();
        }
        int offset = (rowcol.y - _yOff) * _width + (rowcol.x - _xOff);
        offset *= _numVariables;

        System.arraycopy(_buffer, offset, data, 0, _numVariables);
    }

    /**
     * Writes the data to the given location.
     *
     * @param rowcol a Point designating the write location.
     * @param data   the data to be written
     */
    public final void write(Point rowcol, float[] data) throws IOException {
        if (!_hasBuffer) {
            getAndReadBuffer();
        }

        int offset = (rowcol.y - _yOff) * _width + (rowcol.x - _xOff);
        offset *= _numVariables;

        System.arraycopy(data, 0, _buffer, offset, _numVariables);
    }

    /**
     * Closes the quad tree element.
     */
    public final void close() {
    }

    /**
     * Flushes the data of the element to disk.
     */
    public final void flush() throws IOException {
        if (_hasBuffer) {
            FileImageOutputStream outStream = new FileImageOutputStream(_leafFile);
            outStream.seek(0);
            outStream.writeFloats(_buffer, 0, _numFloats);
            outStream.close();
        }
    }

    /**
     * Releases the buffer currently hold.
     */
    public final void releaseBuffer() {
        if (_hasBuffer) {
            _buffer = null;
            _hasBuffer = false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates the disk file associated to this leaf. The file is filled with zeros.
     */
    private void createFile() throws IOException {
        _leafFile = _qtFile.createLeafFile(_layerIndex);
        _leafFile.createNewFile();

        RandomAccessFile randomAccessFile = new RandomAccessFile(_leafFile, "rw");
        randomAccessFile.setLength(_numFloats * 4);
        randomAccessFile.close();
    }

    /**
     * Retrieves a buffer object from the parent and reads the disk file content into the buffer
     */
    private void getAndReadBuffer() throws IOException {
        _buffer = _qtFile.getCacheBuffer(this);
        FileImageInputStream outStream = new FileImageInputStream(_leafFile);
        outStream.seek(0);
        outStream.readFully(_buffer, 0, _numFloats);
        outStream.close();
        _hasBuffer = true;
    }
}
