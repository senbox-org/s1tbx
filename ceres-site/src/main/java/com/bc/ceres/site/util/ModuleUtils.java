package com.bc.ceres.site.util;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Util class which provides methods to remove double Elements from a given array of
 * {@link com.bc.ceres.core.runtime.Module}-objects, to check if a module exists in a dedicated csv-file, to retrieve
 * the year from a module and to retrieve a
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
     * Checks if a module is included on the given inclusion-list.
     *
     * @param module the module to check for
     * @param inclusionList the list, given as csv without line breaks
     * @return true if the module is included
     */
    public static boolean isIncluded(Module module, File inclusionList) {
        try {
            final InputStream stream = new FileInputStream(inclusionList);
            final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{','});
            final String[] allowedModules = csvReader.readRecord();
            return Arrays.asList(allowedModules).contains(module.getSymbolicName());
        } catch (IOException e) {
            // if there is no inclusion list, all modules are displayed 
            return true;
        }
    }

    /**
     * Checks if a module is included within the given inclusion-list
     *
     * @param module the module to check for, may not be null
     * @param inclusionList the list, given as list of strings
     * @return true if the module is included and the inclusion-list is not null
     */
    public static boolean isIncluded(Module module, List<String> inclusionList) {
        return inclusionList == null || inclusionList.contains(module.getSymbolicName());
    }

    /**
     * Parses the year of the modules release from the module.
     *
     * @param module the module
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

}
