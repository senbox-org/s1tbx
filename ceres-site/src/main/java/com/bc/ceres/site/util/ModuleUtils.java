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

/**
 * User: Thomas Storm
 * Date: 03.06.2010
 * Time: 11:03:07
 */
public class ModuleUtils {

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

    public static File retrieveInclusionList(String repositoryUrl) {
        String sep = "/";
        if (repositoryUrl.endsWith(sep)) {
            sep = "";
        }
        return new File(repositoryUrl + sep + InclusionListBuilder.INCLUSION_LIST_FILENAME);
    }
}
