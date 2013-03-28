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

import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
class QuadTreeFile {

    // some property keys
    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";
    private static final String TILE_SIZE_KEY = "tile_size";
    private static final String BUFFERS_KEY = "buffers";
    private static final String VARIABLES_KEY = "num_vars";
    private static final String PROPS_FILE_NAME = "qt.props";

    private File _qtDir;
    private QuadTreeNode _root;

    private int _width;
    private int _height;
    private int _maxTileSize;
    private int _numBuffer;
    private int _numLayers;
    private int _numVariables;
    private int _tileWidth;
    private int _tileHeight;
    private int _currentXMin;
    private int _currentXMax;
    private int _currentYMin;
    private int _currentYMax;

    private final Vector<BufferStorage> _bufferStorage;
    private Properties _props;
    private static final String DATAFILE_DOT_EXTENSION = ".dat";

    /**
     * Constructs the object with default values.
     */
    public QuadTreeFile() {
        _bufferStorage = new Vector<BufferStorage>();
        initMaxSizeFields();
    }


    /**
     * Creates a new TestQuadTreeFile in the assigned directory.
     *
     * @param targetDir    the directory containing the new quad tree file
     * @param width        width of the file in pixels
     * @param height       height of the file in pixels
     * @param maxTileSize  the maximum length of one side of the tiles in pixels
     * @param numBuffer    the number of cache buffers to be used
     * @param numVariables the number of variables contained in one pixel
     */
    public void create(File targetDir, int width, int height, int maxTileSize,
                       int numBuffer, int numVariables) throws IOException {
        ensureValidDirectory(targetDir);
        _width = width;
        _height = height;
        _maxTileSize = maxTileSize;
        _numBuffer = numBuffer;
        _numVariables = numVariables;
        calculateNumLayers();
        createRoot();
        createProperties();
    }

    /**
     * Opens a quad tree file at the given location - if there is one...
     *
     * @param location where the quad tree file resides
     */
    public void open(File location) throws IOException {
        _qtDir = location;
        readProperties();
        calculateNumLayers();
        createBuffer();
        createAndLoadRoot();
    }

    /**
     * Closes the quad tree file.
     */
    public void close() throws IOException {
        writeProperties();
        if (_root != null) {
            _root.close();
            _root = null;
            releaseBuffer();
        }
    }

    /**
     * Flushes the quad tree file.
     */
    public void flush() throws IOException {
        if (_root != null) {
            _root.flush();
        }
    }

    /**
     * Deletes the quad tree file.
     */
    public void delete() throws IOException {
        close();
        FileUtils.deleteTree(_qtDir);
    }

    /**
     * Reads the data from the designated position.
     *
     * @param rowcol <code>Point</code> giving the read position
     * @param data   to be filled with the data (can be null)
     */
    public void read(Point rowcol, float[] data) throws IOException {
        _root.read(rowcol, data);
    }

    /**
     * Writes the data to the location passed in.
     *
     * @param rowcol a <code>Point</code> giving the write location
     * @param data   the data to be written
     */
    public void write(Point rowcol, float[] data) throws IOException {
        traceCoordinateRanges(rowcol);
        _root.write(rowcol, data);
    }

    /**
     * Retrieves the maximum width of file.
     */
    public int getMaxWidth() {
        return _width;
    }

    /**
     * Retrieves the maximum height of file.
     */
    public int getMaxHeight() {
        return _height;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Check whether a given layerID belongs to a leaf or not.
     *
     * @param layerIndex the layer index
     */
    boolean isLeaf(String layerIndex) {
        return layerIndex.length() >= _numLayers;
    }

    /**
     * Creates a leaf file object based on the layer index given.
     *
     * @param layerIndex the layer index of the leaf requesting a file name
     */
    File createLeafFile(String layerIndex) throws IOException {
        return new File(_qtDir, layerIndex + DATAFILE_DOT_EXTENSION);
    }

    /**
     * Retrieves the number of variables used in this file.
     */
    int getNumberOfVariables() {
        return _numVariables;
    }

    /**
     * Retrieves CacheBuffer from the cache handled by the qt file. When no free cache buffer is available: <ul> <li>the
     * oldest buffer is picked (timestamped!)</li> <li>the owner of this buffer is notified to flush it's data to
     * disk</li> <li>the owner of this buffer is notified to release the buffer</li> <li>the requesting leaf is set as
     * the new owner</li> </ul>
     */
    float[] getCacheBuffer(QuadTreeLeaf leaf) throws IOException {
        // get bufferstorage with oldest timestamp
        BufferStorage toChange = getOldestBuffer();

        QuadTreeLeaf oldOwner = toChange.getOwner();

        // if it has holder - call flush on holder
        if (oldOwner != null) {
            oldOwner.flush();
            oldOwner.releaseBuffer();
        }

        // set the leaf passed in as new holder
        toChange.setOwner(leaf);
        return toChange.getBuffer();
    }

    /**
     * Checks whether the directory passed in exists. If not tries to create the directory and write to member field
     * when successful.
     *
     * @param targetDir the directory where the qt shall be created
     */
    private void ensureValidDirectory(File targetDir) throws IOException {
        Guardian.assertNotNull("targetDir", targetDir);

        if (targetDir.exists() && targetDir.isDirectory()) {
            _qtDir = targetDir;
        } else {
            if (targetDir.mkdirs()) {
                _qtDir = targetDir;
            } else {
                throw new IOException("failed to create target directory: " + targetDir.getAbsolutePath());
            }
        }
    }

    /**
     * Calculates the number of layer needed and writes the result to the appropriate member field.
     */
    private void calculateNumLayers() {
        float widthTiles = _width / _maxTileSize;
        float heightTiles = _height / _maxTileSize;
        float layers;

        // get the maximum
        if (widthTiles > heightTiles) {
            layers = widthTiles;
        } else {
            layers = heightTiles;
        }

        // find the next power of two bigger than the layer
        _numLayers = MathUtils.ceilInt(Math.log(layers) / Math.log(2));
        // check for zero on small products - assign at least one layer!
        if (_numLayers < 1) {
            _numLayers = 1;
        }
        int divisor = (int) Math.pow(2, _numLayers);
        _tileWidth = MathUtils.ceilInt(((double) _width) / ((double) divisor));
        _tileHeight = MathUtils.ceilInt(((double) _height) / ((double) divisor));
    }

    /**
     * creates the root node using the coordinates calculated.
     */
    private void createRoot() {
        int divX = _width / 2;
        int divY = _height / 2;

        _root = new QuadTreeNode(divX, divY, _width, _height, this, "");
    }

    /**
     * Creates the root element and loads the file tiles, constructs the tree from the files.
     */
    private void createAndLoadRoot() throws IOException {
        createRoot();

        Vector fileNames = loadFileList();
        // do not perform on load on an empty DB
        if (fileNames.size() > 0) {
            _root.load(fileNames);
        }
    }

    /**
     * Loads the list of the data files in the quad tree directory.
     */
    private Vector loadFileList() {
        String[] filenames = _qtDir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(DATAFILE_DOT_EXTENSION);
            }
        });

        Vector<String> ret = new Vector<String>();
        for (String filename : filenames) {
            ret.add(filename.substring(0, filename.lastIndexOf('.')));
        }

        return ret;
    }

    /**
     * Creates the cache buffers needed.
     */
    private void createBuffer() {
        BufferStorage buffer;
        int size = _tileWidth * _tileHeight * _numVariables;

        // check buffer size
        int numBuf = _width / _tileWidth + 1;
        if (numBuf > _numBuffer) {
            // adapt - we need at least as much buffer as are needed to fill a complete scanline
            _numBuffer = numBuf;
        }

        for (int n = 0; n < _numBuffer; n++) {
            buffer = new BufferStorage(size);
            _bufferStorage.add(buffer);
        }
    }

    /**
     * Releases the cache buffer allocated.
     */
    private void releaseBuffer() {
        BufferStorage buffer;
        QuadTreeLeaf owner;
        // loop over all buffer and tell owners to release
        for (int n = 0; n < _bufferStorage.size(); n++) {
            buffer = _bufferStorage.elementAt(n);
            owner = buffer.getOwner();
            if (owner != null) {
                owner.releaseBuffer();
            }
            buffer.releaseBuffer();
        }
    }

    /**
     * Retrieves the oldest buffer in the storage.
     */
    private BufferStorage getOldestBuffer() {
        long minTimeStamp = Long.MAX_VALUE;
        long currTimeStamp;
        int index = 0;  // first buffer initially
        BufferStorage ret;

        for (int n = 0; n < _numBuffer; n++) {
            ret = _bufferStorage.elementAt(n);
            currTimeStamp = ret.getTimeStamp();
            if (currTimeStamp < minTimeStamp) {
                minTimeStamp = currTimeStamp;
                index = n;
            }
        }

        ret = _bufferStorage.elementAt(index);

        return ret;
    }

    /**
     * Creates the properties file for this quad tree file.
     */
    private void createProperties() throws IOException {
        _props = new Properties();

        _props.setProperty(WIDTH_KEY, String.valueOf(_width));
        _props.setProperty(HEIGHT_KEY, String.valueOf(_height));
        _props.setProperty(TILE_SIZE_KEY, String.valueOf(_maxTileSize));
        _props.setProperty(BUFFERS_KEY, String.valueOf(_numBuffer));
        _props.setProperty(VARIABLES_KEY, String.valueOf(_numVariables));

        File propsFileName = new File(_qtDir, PROPS_FILE_NAME);
        FileOutputStream outStream = new FileOutputStream(propsFileName);

        _props.store(outStream, null);

        outStream.close();
    }

    private void writeProperties() throws IOException {
        _props.setProperty(WIDTH_KEY, String.valueOf(_width));
        _props.setProperty(HEIGHT_KEY, String.valueOf(_height));
        _props.setProperty(TILE_SIZE_KEY, String.valueOf(_maxTileSize));
        _props.setProperty(BUFFERS_KEY, String.valueOf(_numBuffer));
        _props.setProperty(VARIABLES_KEY, String.valueOf(_numVariables));

        File propsFileName = new File(_qtDir, PROPS_FILE_NAME);
        FileOutputStream outStream = new FileOutputStream(propsFileName);

        _props.store(outStream, null);

        outStream.close();
    }

    /**
     * Reads the properties file in the quad tree directory.
     */
    private void readProperties() throws IOException {
        File propsFileName = new File(_qtDir, PROPS_FILE_NAME);

        if (_props != null) {
            _props.clear();
        } else {
            _props = new Properties();
        }

        // get them from disk
        FileInputStream inStream = new FileInputStream(propsFileName);
        _props.load(inStream);
        inStream.close();

        String propVal;
        propVal = getPropertySafe(WIDTH_KEY);
        _width = Integer.parseInt(propVal);

        propVal = getPropertySafe(HEIGHT_KEY);
        _height = Integer.parseInt(propVal);

        propVal = getPropertySafe(TILE_SIZE_KEY);
        _maxTileSize = Integer.parseInt(propVal);

        propVal = getPropertySafe(BUFFERS_KEY);
        _numBuffer = Integer.parseInt(propVal);

        propVal = getPropertySafe(VARIABLES_KEY);
        _numVariables = Integer.parseInt(propVal);
    }

    /**
     * Reads the property with given key, throws exception when no property is present
     */
    private String getPropertySafe(String key) throws IOException {
        String strRet = _props.getProperty(key);
        if (strRet == null) {
            throw new IOException("corrupted quad tree file: unable to read property '" + key + "'");
        }
        return strRet;
    }

    private void initMaxSizeFields() {
        _currentXMin = Integer.MAX_VALUE;
        _currentXMax = Integer.MIN_VALUE;
        _currentYMin = Integer.MAX_VALUE;
        _currentYMax = Integer.MIN_VALUE;
    }

    private void traceCoordinateRanges(Point rowcol) {
        if (rowcol.x < _currentXMin) {
            _currentXMin = rowcol.x;
        }
        if (rowcol.x > _currentXMax) {
            _currentXMax = rowcol.x;
        }
        if (rowcol.y < _currentYMin) {
            _currentYMin = rowcol.y;
        }
        if (rowcol.y > _currentYMax) {
            _currentYMax = rowcol.y;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// INNER CLASS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Internal class serves for memory caching.
     */
    class BufferStorage {

        float[] _buffer;
        QuadTreeLeaf _owner;
        long _timeStamp;

        /**
         * Creates a new object with given buffer size.
         *
         * @param bufferSize the size of the buffer in elements
         */
        public BufferStorage(int bufferSize) {
            _buffer = new float[bufferSize];
            // initial timestamp
            _timeStamp = System.currentTimeMillis();
        }

        /**
         * Retrieves the time stamp of the buffer (i.e. the last access time).
         */
        public long getTimeStamp() {
            return _timeStamp;
        }

        /**
         * Retrieves the owner of the buffer.
         */
        public QuadTreeLeaf getOwner() {
            return _owner;
        }

        /**
         * Sets a new owner of the object.
         *
         * @param newOwner the new owner
         */
        public void setOwner(QuadTreeLeaf newOwner) {
            _owner = newOwner;
            _timeStamp = System.currentTimeMillis();
        }

        /**
         * Retrieves the buffer contained in the objects.
         */
        public float[] getBuffer() {
            return _buffer;
        }

        /**
         * Releases the buffer.
         */
        public void releaseBuffer() {
            _buffer = null;
        }
    }
}
