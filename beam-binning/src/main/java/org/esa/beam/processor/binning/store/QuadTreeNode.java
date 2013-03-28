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

import java.awt.Point;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
final class QuadTreeNode implements QuadTreeElement {

    private final QuadTreeFile _qtFile;
    private final int _divX;
    private final int _divY;
    private final int _width;
    private final int _height;
    private final String _layer;

    private QuadTreeElement _nw;
    private QuadTreeElement _sw;
    private QuadTreeElement _ne;
    private QuadTreeElement _se;

    /**
     * Constructs the object with given parameters.
     *
     * @param divX       the x coordinate division line index
     * @param divY       the y coordinate division line index
     * @param width      width of this QuadTreeNode
     * @param height     the height of this QuadTreeNode
     * @param qtFile     the QuadTreeFile containing this element
     * @param layerIndex the index of the layer where this element is attached to the tree
     */
    public QuadTreeNode(int divX, int divY, int width, int height, QuadTreeFile qtFile, String layerIndex) {
        _qtFile = qtFile;
        _divX = divX;
        _divY = divY;
        _layer = layerIndex;
        _width = width;
        _height = height;
    }

    /**
     * Creates the node and loads eventually the datafile attached to it (if it is a leaf)
     */
    public final void load(Vector fileNames) throws IOException {
        // calculate the decimal position at which to search
        int decimal = _layer.length();
        int pos;
        String fileName;
        String toSearch;
        String subString;

        // search nw tile
        toSearch = '1' + _layer;
        for (int n = 0; n < fileNames.size(); n++) {
            fileName = (String) fileNames.get(n);
            pos = fileName.length() - 1 - decimal;
            subString = fileName.substring(pos, fileName.length());
            if (toSearch.equalsIgnoreCase(subString)) {
                _nw = new_NW_Element(true);
                _nw.load(fileNames);
                break;
            }
        }

        // search ne tile
        toSearch = '2' + _layer;
        for (int n = 0; n < fileNames.size(); n++) {
            fileName = (String) fileNames.get(n);
            pos = fileName.length() - 1 - decimal;
            subString = fileName.substring(pos, fileName.length());
            if (toSearch.equalsIgnoreCase(subString)) {
                _ne = new_NE_Element(true);
                _ne.load(fileNames);
                break;
            }
        }

        // search se tile
        toSearch = '3' + _layer;
        for (int n = 0; n < fileNames.size(); n++) {
            fileName = (String) fileNames.get(n);
            pos = fileName.length() - 1 - decimal;
            subString = fileName.substring(pos, fileName.length());
            if (toSearch.equalsIgnoreCase(subString)) {
                _se = new_SE_Element(true);
                _se.load(fileNames);
                break;
            }
        }

        // search sw tile
        toSearch = '4' + _layer;
        for (int n = 0; n < fileNames.size(); n++) {
            fileName = (String) fileNames.get(n);
            pos = fileName.length() - 1 - decimal;
            subString = fileName.substring(pos, fileName.length());
            if (toSearch.equalsIgnoreCase(subString)) {
                _sw = new_SW_Element(true);
                _sw.load(fileNames);
                break;
            }
        }
    }

    /**
     * Reads the data at the given location.
     *
     * @param rowcol  a Point designating the read location.
     * @param data array to be filled
     */
    public final void read(Point rowcol, float[] data) throws IOException {
        QuadTreeElement whereFrom = getElement(rowcol);

        if (whereFrom == null) {
            Arrays.fill(data, 0f);
        }else{
            whereFrom.read(rowcol, data);
        }
    }

    /**
     * Writes a bin to the given location.
     *
     * @param rowcol a Point designating the write location.
     * @param data    the data to be written
     */
    public final void write(Point rowcol, float[] data) throws IOException {
        QuadTreeElement whereTo = getElement(rowcol);

        if (whereTo == null) {
            whereTo = createElement(rowcol);
        }

        whereTo.write(rowcol, data);
    }

    /**
     * Closes the quad tree element.
     */
    public final void close() throws IOException {
        if (_nw != null) {
            _nw.close();
        }
        if (_ne != null) {
            _ne.close();
        }
        if (_se != null) {
            _se.close();
        }
        if (_sw != null) {
            _sw.close();
        }
    }

    /**
     * Flushes the data of the element to disk.
     */
    public final void flush() throws IOException {
        if (_nw != null) {
            _nw.flush();
        }
        if (_ne != null) {
            _ne.flush();
        }
        if (_se != null) {
            _se.flush();
        }
        if (_sw != null) {
            _sw.flush();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the quad tree subelement for the position passed in
     *
     * @param rowcol the position to be accessed
     */
    private QuadTreeElement getElement(Point rowcol) {
        if (rowcol.x < _divX) {
            if (rowcol.y < _divY) {
                return _nw;
            } else {
                return _sw;
            }
        } else {
            if (rowcol.y < _divY) {
                return _ne;
            } else {
                return _se;
            }
        }
    }

    /**
     * Creates the according element for the location and layer index.
     *
     * @param rowcol the position for which the element shall be created
     */
    private QuadTreeElement createElement(Point rowcol) throws IOException {
        QuadTreeElement ret;

        if (rowcol.x < _divX) {
            if (rowcol.y < _divY) {
                ret = new_NW_Element(false);
            } else {
                ret = new_SW_Element(false);
            }
        } else {
            if (rowcol.y < _divY) {
                ret = new_NE_Element(false);
            } else {
                ret = new_SE_Element(false);
            }
        }

        return ret;
    }

    /**
     * Creates a <code>QuadTreeElement</code> for the northwest position and adds it to the internal fields.
     *
     * @param load boolean whether to load an existing element or to create a new one
     */
    private QuadTreeElement new_NW_Element(boolean load) throws IOException {
        int newWidth = _width / 2;
        int newHeight = _height / 2;
        int newDivX = _divX - (newWidth - newWidth / 2);
        int newDivY = _divY - (newHeight - newHeight / 2);
        String newLayerIndex = createLayerIndex(1);

        if (_qtFile.isLeaf(newLayerIndex)) {
            QuadTreeLeaf leaf = new QuadTreeLeaf(_divX - newWidth, _divY - newHeight, newWidth, newHeight, _qtFile,
                                                 newLayerIndex);
            if (!load) {
                leaf.create();
            }
            _nw = leaf;
        } else {
            _nw = new QuadTreeNode(newDivX, newDivY, newWidth, newHeight, _qtFile, newLayerIndex);
        }

        return _nw;
    }

    /**
     * Creates a <code>QuadTreeElement</code> for the northeast position and adds it to the internal fields.
     *
     * @param load boolean whether to load an existing element or to create a new one
     */
    private QuadTreeElement new_NE_Element(boolean load) throws IOException {
        int newWidth = _width - _width / 2;
        int newHeight = _height / 2;
        int newDivX = _divX + newWidth / 2;
        int newDivY = _divY - (newHeight - newHeight / 2);
        String newLayerIndex = createLayerIndex(2);

        if (_qtFile.isLeaf(newLayerIndex)) {
            QuadTreeLeaf leaf = new QuadTreeLeaf(_divX, _divY - newHeight, newWidth, newHeight, _qtFile, newLayerIndex);
            if (!load) {
                leaf.create();
            }
            _ne = leaf;
        } else {
            _ne = new QuadTreeNode(newDivX, newDivY, newWidth, newHeight, _qtFile, newLayerIndex);
        }

        return _ne;
    }

    /**
     * Creates a <code>QuadTreeElement</code> for the southeast position and adds it to the internal fields.
     *
     * @param load boolean whether to load an existing element or to create a new one
     */
    private QuadTreeElement new_SE_Element(boolean load) throws IOException {
        int newWidth = _width - _width / 2;
        int newHeight = _height - _height / 2;
        int newDivX = _divX + newWidth / 2;
        int newDivY = _divY + newHeight / 2;
        String newLayerIndex = createLayerIndex(3);

        if (_qtFile.isLeaf(newLayerIndex)) {
            QuadTreeLeaf leaf = new QuadTreeLeaf(_divX, _divY, newWidth, newHeight, _qtFile, newLayerIndex);
            if (!load) {
                leaf.create();
            }
            _se = leaf;
        } else {
            _se = new QuadTreeNode(newDivX, newDivY, newWidth, newHeight, _qtFile, newLayerIndex);
        }

        return _se;
    }

    /**
     * Creates a <code>QuadTreeElement</code> for the southwest position and adds it to the internal fields.
     *
     * @param load boolean whether to load an existing element or to create a new one
     */
    private QuadTreeElement new_SW_Element(boolean load) throws IOException {
        int newWidth = _width / 2;
        int newHeight = _height - _height / 2;
        int newDivX = _divX - (newWidth - newWidth / 2);
        int newDivY = _divY + newHeight / 2;
        String newLayerIndex = createLayerIndex(4);

        if (_qtFile.isLeaf(newLayerIndex)) {
            QuadTreeLeaf leaf = new QuadTreeLeaf(_divX - newWidth, _divY, newWidth, newHeight, _qtFile, newLayerIndex);
            if (!load) {
                leaf.create();
            }
            _sw = leaf;
        } else {
            _sw = new QuadTreeNode(newDivX, newDivY, newWidth, newHeight, _qtFile, newLayerIndex);
        }

        return _sw;
    }

    /**
     * Creates a layer index from the laer position identifier passed in and the current layer where this object is
     * attached to the tree.
     *
     * @param layerId the layer identifier
     */
    private String createLayerIndex(int layerId) {
        return Integer.toString(layerId) +  _layer;
    }
}
