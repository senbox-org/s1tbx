/*
 * Copyright (C) 2015 CS SI
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

package org.esa.snap.configurator;


/**
 * Storage for performance parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class PerformanceParameters {

    private long vmXMX;
    private long vmXMS;
    private String vmTmpDir;
    private String auxDataPath;
    private String largeTileCache;

    private String otherVMOptions;


    /**
     *
     * @return a clone of this performance parameter object
     */
    public PerformanceParameters clone() {
        PerformanceParameters clone = new PerformanceParameters();
        clone.setVmTmpDir(this.vmTmpDir);
        clone.setAuxDataPath(this.auxDataPath);
        clone.setVmXMS(this.vmXMS);
        clone.setVmXMX(this.vmXMX);
        clone.setLargeTileCache(this.largeTileCache);
        return clone;
    }


    // TODO: supress this when parameters are functional
    // for stubbing only
    private static PerformanceParameters actualParameters = null;

    public long getVmXMX() {
        return vmXMX;
    }

    public void setVmXMX(long vmXMX) {
        this.vmXMX = vmXMX;
    }

    public long getVmXMS() {
        return vmXMS;
    }

    public void setVmXMS(long vmXMS) {
        this.vmXMS = vmXMS;
    }

    public String getOtherVMOptions() {
        return otherVMOptions;
    }

    public String getVmTmpDir() {
        return vmTmpDir;
    }

    public void setVmTmpDir(String vmTmpDir) {
        this.vmTmpDir = vmTmpDir;
    }

    public String getAuxDataPath() {
        return auxDataPath;
    }

    public void setAuxDataPath(String auxDataPath) {
        this.auxDataPath = auxDataPath;
    }

    public String getLargeTileCache() {
        return largeTileCache;
    }

    public void setLargeTileCache(String largeTileCache) {
        this.largeTileCache = largeTileCache;
    }

    public String getVMParameters() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(" -Xmx ");
        buffer.append(getVmXMX());
        buffer.append(" - Xms ");
        buffer.append(getVmXMS());
        buffer.append(" ");
        buffer.append(getOtherVMOptions());
        return buffer.toString();
    }


    /**
     *
     * Reads the parameters files to retreive the actual performance parameters.
     *
     * @return the actual performance parameters
     */
    public static PerformanceParameters retreiveActualConfiguration() {

        if(actualParameters == null) {
            actualParameters = new PerformanceParameters();

            actualParameters.setVmTmpDir("C:\\Tmp");
            actualParameters.setLargeTileCache("C:\\Tmp");
            actualParameters.setVmXMX(4096);
            actualParameters.setAuxDataPath("C:\\Tmp");
            actualParameters.setVmXMS(1024);
        }

        return actualParameters;
    }

    /**
     *
     * @param newParameters
     */
    public static void setActualParameters(PerformanceParameters newParameters) {
        PerformanceParameters.actualParameters = newParameters;
    }
}
