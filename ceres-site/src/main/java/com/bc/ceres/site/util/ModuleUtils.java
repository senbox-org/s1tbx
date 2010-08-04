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

package com.bc.ceres.site.util;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Version;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bc.ceres.site.util.ExclusionListBuilder.*;

/**
 * Utility class which provides methods to remove double Elements from a given array of
 * {@link com.bc.ceres.core.runtime.Module}-objects, to check if a module exists in a dedicated csv-file, to retrieve
 * the year from a module and to retrieve if a module is on the exclusion list
 *
 * @author Thomas Storm
 * @version 1.0
 */
public class ModuleUtils {

    /**
     * Removes double modules from a given array. Modules are equal in this sense if they match in their symbolic name.
     * If the array contains two modules which are equal in that sense, the module with the <b>lower</b> version number
     * is removed.
     *
     * @param modules the module array which shall be cleansed of double modules.
     *
     * @return the revised module array
     */
    public static Module[] removeDoubles(Module[] modules) {

        final ArrayList<Module> temp = new ArrayList<Module>();
        temp.addAll(Arrays.asList(modules));

        final ArrayList<Module> removeList = new ArrayList<Module>();

        for (Module module : modules) {
            final String symbolicName = module.getSymbolicName();
            final Version version = module.getVersion();
            temp.remove(module);
            for (Module testModule : temp) {
                final boolean isHigherOrEqualVersion = version.compareTo(testModule.getVersion()) < 1;
                if (testModule.getSymbolicName().equals(symbolicName) && isHigherOrEqualVersion) {
                    removeList.add(module);
                }
            }
            if (!removeList.contains(module)) {
                temp.add(module);
            }
        }

        temp.removeAll(removeList);
        return temp.toArray(new Module[temp.size()]);
    }

    /**
     * Checks if a module is excluded on the given exclusion-list.
     *
     * @param module          the module to check for
     * @param excludedModules the list of excluded modules
     *
     * @return true if the module is excluded
     */
    public static boolean isExcluded(Module module, String[] excludedModules) {
        if (excludedModules == null) {
            return false;
        }
        List<String> stringList = Arrays.asList(excludedModules);
        return stringList.contains(module.getSymbolicName());
    }

    /**
     * Parses the year of the modules release from the module.
     *
     * @param module the module
     *
     * @return the year
     */
    public static String retrieveYear(Module module) {
        String copyright;
        String year = "-1";
        if ((copyright = module.getCopyright()) != null) {
            copyright = copyright.toLowerCase().replace("copyright", "");
            copyright = copyright.toLowerCase().replace("(c)", "");
            int endIndex = copyright.indexOf(" by");
            year = copyright.substring(0, endIndex);
        }
        return year;
    }

    /**
     * Retrieves the size of the module-jar
     *
     * @param module the module to determine the size of
     *
     * @return the size, rounded to megabyte or kilobyte
     */
    public static String retrieveSize(Module module) {
        final long bytes = module.getContentLength();
        long kilos = Math.round(bytes / 1024.0);
        long megas = Math.round(bytes / (1024.0 * 1024.0));
        if (megas > 0) {
            return megas + " MB";
        } else if (kilos > 0) {
            return kilos + " KB";
        } else {
            return "< 1 KB";
        }
    }

    /**
     * Excludes double modules and modules which are on the given exclusion list file; returns a sorted list
     *
     * @param modules             the modules to clean up
     * @param exclusionListReader a reader on the list containing modules to be excluded from the view
     *
     * @return the cleaned-up list of modules
     */
    public static Module[] cleanModules(Module[] modules, Reader exclusionListReader) {
        if (exclusionListReader == null) {
            return modules;
        }
        final ArrayList<Module> removeList = new ArrayList<Module>();
        final CsvReader csvReader = new CsvReader(exclusionListReader, CSV_SEPARATOR_ARRAY);
        final String[] excludedModules;
        try {
            excludedModules = csvReader.readRecord();
        } catch (IOException e) {
            return modules;
        }
        for (Module module : modules) {
            if (isExcluded(module, excludedModules)) {
                removeList.add(module);
            }
        }
        final ArrayList<Module> temp = new ArrayList<Module>();
        temp.addAll(Arrays.asList(modules));
        temp.removeAll(removeList);
        Collections.sort(temp);
        return removeDoubles(temp.toArray(new Module[temp.size()]));
    }

    /**
     * Tests if a module (given by its symbolic name) is listed on the exclusion list
     *
     * @param module              the module to test, represented by its symbolic name
     * @param exclusionListReader a reader on the list of exclusions
     *
     * @return true if the module is listed on the list
     */
    public static boolean isExcluded(String module, Reader exclusionListReader) {
        try {
            final CsvReader csvReader = new CsvReader(exclusionListReader, CSV_SEPARATOR_ARRAY);
            final String[] allowedModules = csvReader.readRecord();
            if (allowedModules != null) {
                return Arrays.asList(allowedModules).contains(module);
            }
            return false;
        } catch (IOException e) {
            // if there is no inclusion list, all modules are displayed
            return true;
        }
    }

    /**
     * Returns the real name of a module given by its symbolic name; if it is not found, the symbolic name is returned
     *
     * @param symbolicName the symbolic name of the module
     * @param modules      the array of available modules
     *
     * @return the real name of the module, if found; else its symbolic name
     */
    public static String symbolicToReadableName(String symbolicName, Module[] modules) {
        for (Module module : modules) {
            if (module.getSymbolicName().equals(symbolicName)) {
                return module.getName();
            }
        }
        return symbolicName;
    }
}
