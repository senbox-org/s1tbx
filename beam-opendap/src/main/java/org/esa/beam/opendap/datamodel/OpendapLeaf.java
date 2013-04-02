/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.datamodel;

import thredds.catalog.InvDataset;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
* This is a container for opendap-leaf related information.
*
* @author Sabine Embacher
* @author Tonio Fincke
*
*/
public class OpendapLeaf extends OpendapNode {

    private boolean dapAccess;
    private boolean fileAccess;
    private String dapUri;
    private String fileUri;
    private int fileSize;
    private Set<DAPVariable> variables;

    public OpendapLeaf(String name, InvDataset dataset) {
        super(name, dataset);
        this.variables = new HashSet<DAPVariable>();
    }

    public boolean isDapAccess() {
        return dapAccess;
    }

    public boolean isFileAccess() {
        return fileAccess;
    }

    public String getDasUri() {
        return getDapUri() + ".das";
    }

    public String getDdsUri() {
        return getDapUri() + ".dds";
    }

    public String getDdxUri() {
        return getDapUri() + ".ddx";
    }

    public String getDapUri() {
        return dapUri;
    }

    public void setDapUri(String dapUri) {
        this.dapUri = dapUri;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public void setDapAccess(boolean dapAccess) {
        this.dapAccess = dapAccess;
    }

    public void setFileAccess(boolean fileAccess) {
        this.fileAccess = fileAccess;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public DAPVariable[] getDAPVariables(){
        return variables.toArray(new DAPVariable[variables.size()]);
    }

    public void addDAPVariable(DAPVariable variable){
        variables.add(variable);
    }

    public void addDAPVariables(DAPVariable[] dapVariables) {
        Collections.addAll(variables, dapVariables);
    }
}
