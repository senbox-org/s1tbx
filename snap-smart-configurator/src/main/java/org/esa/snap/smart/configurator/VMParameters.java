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

package org.esa.snap.smart.configurator;

import oracle.jrockit.jfr.StringConstantPool;
import org.apache.commons.lang.StringEscapeUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class to store and manipulate VM Parameters
 *
 * @author Nicolas Ducoin
 */
public class VMParameters {

    private static final String DEFAULT_OPTION_PROPERTY_KEY="default_options";

    private static Path snapConfigPath = null;
    private static Path gptVmOptionsPath = null;
    private static Path pconvertVmOptionsPath = null;

    private long vmXMX = 0;
    private long vmXMS = 0;
    private String otherVMOptions;

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

    public void setOtherVMOptions(String otherVMOptions) {
        this.otherVMOptions = otherVMOptions;
    }

    public VMParameters(String vmParametersString) {
        this(VMParameters.toParamList(vmParametersString));
    }

    public VMParameters(List<String> vmParametersStringList){

        if(vmParametersStringList != null) {
            fromStringList(vmParametersStringList);
        }
    }

    static void setSnapConfigPath(Path snapConfigPath) {
        VMParameters.snapConfigPath = snapConfigPath;
    }

    public static Path getSnapConfigPath() {
        if(snapConfigPath == null) {
            snapConfigPath = Config.instance().installDir().resolve("etc").resolve("snap.conf");
        }
        return snapConfigPath;
    }

    public static Path getGptVmOptionsPath() {
        if(gptVmOptionsPath == null) {
            gptVmOptionsPath = Config.instance().installDir().resolve("bin").resolve("gpt.vmoptions");
        }
        return gptVmOptionsPath;
    }

    public static Path getPconvertVmOptionsPath() {
        if(pconvertVmOptionsPath == null) {
            pconvertVmOptionsPath = Config.instance().installDir().resolve("bin").resolve("pconvert.vmoptions");
        }
        return pconvertVmOptionsPath;
    }

    public void fromStringList(List<String> vmParametersStringArray) {
        String otherVMParams = "";
        for (String thisArg : vmParametersStringArray) {

            if (thisArg != null) {
                if (thisArg.startsWith("-Xmx")) {
                    try {
                        setVmXMX(getMemVmSettingValue(thisArg));
                    } catch (NumberFormatException ex) {
                        SystemUtils.LOG.warning("VM Parameters, bad XMX: " + thisArg);
                    }
                } else if (thisArg.startsWith("-Xms")) {
                    try {
                        setVmXMS(getMemVmSettingValue(thisArg));
                    } catch (NumberFormatException ex) {
                        SystemUtils.LOG.warning("VM Parameters, bad XMS: " + thisArg);
                    }
                } else if (!thisArg.isEmpty()){
                    otherVMParams += thisArg + " ";
                }
            }
            setOtherVMOptions(otherVMParams);
        }
    }


    private long getMemVmSettingValue(String vmStringSetting) throws NumberFormatException{

        String memStringValue = vmStringSetting.substring(4);
        double multValue;
        if(memStringValue.endsWith("g") || memStringValue.endsWith("G")) {
            multValue = 1024;
            memStringValue = memStringValue.substring(0, memStringValue.length()-1);
        } else if(memStringValue.endsWith("m") || memStringValue.endsWith("M")) {
            multValue = 1;
            memStringValue = memStringValue.substring(0, memStringValue.length()-1);
        } else if(memStringValue.endsWith("k") || memStringValue.endsWith("K")) {
            multValue = 1/1024;
            memStringValue = memStringValue.substring(0, memStringValue.length()-1);
        } else {
            multValue = 1/(1024*1024);
        }

        return Math.round(Long.parseLong(memStringValue) * multValue);
    }

    static List<String> toParamList(String parametersAsString) {
        List<String> vmParamsList = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'| \"([^\"]*)\"| '([^']*)'");
        Matcher regexMatcher = regex.matcher(parametersAsString);
        while (regexMatcher.find()) {
            if (!vmParamsList.isEmpty() &&
                    (regexMatcher.group(1) != null || regexMatcher.group(2) != null) &&
                    regexMatcher.group(3) == null &&
                    regexMatcher.group(4) == null) {
                int lastAddedIndex = vmParamsList.size()-1;
                String lastParam = vmParamsList.get(lastAddedIndex) + regexMatcher.group();
                vmParamsList.set(lastAddedIndex, lastParam);
            }  else {
                vmParamsList.add(regexMatcher.group());
            }

        }
        return vmParamsList;
    }

    static String toString(List<String> parametersAsList) {
        StringBuilder builder = new StringBuilder();
        for(String parameter : parametersAsList) {
            builder.append(parameter);
            builder.append(' ');
        }
        return builder.toString();
    }


    static VMParameters load() {
        Properties properties = loadSnapConfProperties();

        String defaultParameters = properties.getProperty(DEFAULT_OPTION_PROPERTY_KEY);

        List<String> vmParameters = null;
        if(defaultParameters != null) {
            if (defaultParameters.startsWith("\"")) {
                // we remove global the double quotes
                defaultParameters = defaultParameters.substring(1, defaultParameters.length() - 1);
            }
            List<String> defaultParametersAsList = VMParameters.toParamList(defaultParameters);
            vmParameters = new ArrayList<>(defaultParametersAsList.size());
            for (String parameter : defaultParametersAsList) {
                if (parameter.startsWith("-J")) {
                    vmParameters.add(parameter.substring(2));
                }
            }
        }

        return new VMParameters(vmParameters);
    }

    private static Properties loadSnapConfProperties() {

        Properties properties = new Properties();
        try {
            try (BufferedReader reader = Files.newBufferedReader(getSnapConfigPath())) {
                properties.load(reader);
            }
        } catch (IOException e) {
            // we could not find the snap config file. We log an error and continue, it will be created
            SystemUtils.LOG.severe(String.format("Can't load snap config file '%s'", getSnapConfigPath().toString()));
        }

        return properties;
    }

    private static ArrayList<String> loadGptParameterList() {

        List<String> list = new ArrayList<>();
        try (Stream<String> stream = Files.lines(getGptVmOptionsPath())) {

            list = stream
                    .filter(line -> line.startsWith("#"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            SystemUtils.LOG.severe(String.format("Can't load gpt vmoptions file '%s'", getGptVmOptionsPath().toString()));
        }
        return (ArrayList<String>) list;
    }

    private static ArrayList<String> loadPconvertParameterList() {

        List<String> list = new ArrayList<>();
        try (Stream<String> stream = Files.lines(getPconvertVmOptionsPath())) {

            list = stream
                    .filter(line -> line.startsWith("#"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            SystemUtils.LOG.severe(String.format("Can't load pconvert vmoptions file '%s'", getPconvertVmOptionsPath().toString()));
        }
        return (ArrayList<String>) list;
    }

    /**
     * Check if the current user can save the VM parameters
     *
     * @return true if the VM parameters can be saved
     */
    public static boolean canSave() {
        return Files.isWritable(getSnapConfigPath());
    }

    /**
     * If allowed, save the VM parameters to disk so they can be used next time
     *
     * @throws IOException if the VM parameters could not be saved
     */
    void save(boolean saveGPT, boolean savePConvert) throws IOException {

        Properties properties = loadSnapConfProperties();

        String defaultParameters = properties.getProperty(DEFAULT_OPTION_PROPERTY_KEY);

        ArrayList<String> parametersToSave = new ArrayList<>();

        if(defaultParameters != null) {
            // We search for parameters not starting with "-J" in the actual default parameters list.
            // These are not VM parameters and so they will not be replaced
            if(defaultParameters.startsWith("\"")) {
                // we remove global the double quotes
                defaultParameters = defaultParameters.substring(1, defaultParameters.length()-1);
            }
            List<String> defaultParametersAsList = toParamList(defaultParameters);
            for (String defaultParameter : defaultParametersAsList) {
                if (!defaultParameter.startsWith("-J")) {
                    parametersToSave.add(defaultParameter);
                }
            }
        }

        // We add these VM Parameters, adding "-J" to the list
        List<String> vmParametersAsList = toParamList(toString());
        for(String vmParameter : vmParametersAsList) {
            parametersToSave.add("-J" + vmParameter);
        }

        String vmParametersAsString = VMParameters.toString(parametersToSave);
        vmParametersAsString = StringEscapeUtils.escapeJava(vmParametersAsString);
        vmParametersAsString = "\"" + vmParametersAsString + "\"";
        String defaultOptionAsString = DEFAULT_OPTION_PROPERTY_KEY + "=" + vmParametersAsString;

        // we replace the default option setting in the config path
        // we can't use properties.store since this doesn't keep comments and creates some problems with paths
        List<String> snapConfigLines = Files.readAllLines(getSnapConfigPath());

        if(snapConfigLines != null && snapConfigLines.size() > 0) {
            Iterator<String> configLinesIterator = snapConfigLines.iterator();
            String regex = DEFAULT_OPTION_PROPERTY_KEY + "[ =:].*";

            BufferedWriter writer = Files.newBufferedWriter(getSnapConfigPath());
            do {

                String configLine = configLinesIterator.next();

                snapConfigLines.iterator();

                if (configLine != null) {
                    if (configLine.matches(regex)) {
                        while (configLine != null && configLine.trim().endsWith("\\")) {
                            configLine = configLinesIterator.next();
                        }
                        writer.write(defaultOptionAsString);
                    } else {
                        writer.write(configLine);
                    }
                    writer.newLine();
                }
            } while (configLinesIterator.hasNext());

            writer.close();
        }
    }

    void saveToVMOptions(Path vmOptionsPath) throws IOException {
        ArrayList<String> parametersToSave = new ArrayList<>();

        if(getOtherVMOptions() != null && getOtherVMOptions().length() > 0) {
            String[] others = getOtherVMOptions().split(" ");
            for(String other : others) {
                parametersToSave.add(other);
            }
        }

        // we replace the default option setting in the config path
        // we can't use properties.store since this doesn't keep comments and creates some problems with paths
        List<String> vmConfigLines = Files.readAllLines(vmOptionsPath);

        boolean xmxReplaced = false;
        boolean xmsReplaced = false;
        if(vmConfigLines != null && vmConfigLines.size() > 0) {
            Iterator<String> configLinesIterator = vmConfigLines.iterator();
            String regex = DEFAULT_OPTION_PROPERTY_KEY + "[ =:].*";

            BufferedWriter writer = Files.newBufferedWriter(vmOptionsPath);
            do {

                String configLine = configLinesIterator.next();

                if (configLine != null) {
                    if (configLine.startsWith("-Xmx") && vmXMX != 0) {
                        writer.write("-Xmx" + getVmXMX() + "m");
                        xmxReplaced = true;
                    } else if (configLine.startsWith("-Xms") && vmXMS != 0) {
                        writer.write("-Xms" + getVmXMS() + "m");
                        xmsReplaced = true;
                    } else {
                        boolean found = false;
                        for(String parameterToSave : parametersToSave) {
                            String[] split = parameterToSave.split("=");
                            if(split == null || split.length == 0) {
                                continue;
                            }
                            if (configLine.startsWith(split[0])) {
                                writer.write(parameterToSave);
                                parametersToSave.remove(parameterToSave);
                                found = true;
                                break;
                            }
                        }
                        if(!found) {
                            writer.write(configLine);
                        }
                    }
                    writer.newLine();
                }
            } while (configLinesIterator.hasNext());

            if(!xmsReplaced) {
                writer.write("-Xms" + getVmXMS() + "m");
                writer.newLine();
            }
            if(!xmxReplaced) {
                writer.write("-Xmx" + getVmXMX() + "m");
                writer.newLine();
            }
            for(String parameterToSave : parametersToSave) {
                writer.write(parameterToSave);
                writer.newLine();
            }
            writer.close();
        }
    }


    void save() throws IOException {

        Properties properties = loadSnapConfProperties();

        String defaultParameters = properties.getProperty(DEFAULT_OPTION_PROPERTY_KEY);

        ArrayList<String> parametersToSave = new ArrayList<>();

        if(defaultParameters != null) {
            // We search for parameters not starting with "-J" in the actual default parameters list.
            // These are not VM parameters and so they will not be replaced
            if(defaultParameters.startsWith("\"")) {
                // we remove global the double quotes
                defaultParameters = defaultParameters.substring(1, defaultParameters.length()-1);
            }
            List<String> defaultParametersAsList = toParamList(defaultParameters);
            for (String defaultParameter : defaultParametersAsList) {
                if (!defaultParameter.startsWith("-J")) {
                    parametersToSave.add(defaultParameter);
                }
            }
        }

        // We add these VM Parameters, adding "-J" to the list
        List<String> vmParametersAsList = toParamList(toString());
        for(String vmParameter : vmParametersAsList) {
            parametersToSave.add("-J" + vmParameter);
        }

        String vmParametersAsString = VMParameters.toString(parametersToSave);
        vmParametersAsString = StringEscapeUtils.escapeJava(vmParametersAsString);
        vmParametersAsString = "\"" + vmParametersAsString + "\"";
        String defaultOptionAsString = DEFAULT_OPTION_PROPERTY_KEY + "=" + vmParametersAsString;

        // we replace the default option setting in the config path
        // we can't use properties.store since this doesn't keep comments and creates some problems with paths
        List<String> snapConfigLines = Files.readAllLines(getSnapConfigPath());

        if(snapConfigLines != null && snapConfigLines.size() > 0) {
            Iterator<String> configLinesIterator = snapConfigLines.iterator();
            String regex = DEFAULT_OPTION_PROPERTY_KEY + "[ =:].*";

            BufferedWriter writer = Files.newBufferedWriter(getSnapConfigPath());
            do {

                String configLine = configLinesIterator.next();

                snapConfigLines.iterator();

                if (configLine != null) {
                    if (configLine.matches(regex)) {
                        while (configLine != null && configLine.trim().endsWith("\\")) {
                            configLine = configLinesIterator.next();
                        }
                        writer.write(defaultOptionAsString);
                    } else {
                        writer.write(configLine);
                    }
                    writer.newLine();
                }
            } while (configLinesIterator.hasNext());

            writer.close();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(getVmXMX() != 0) {
            builder.append(" -Xmx");
            builder.append(getVmXMX());
            builder.append("m ");
        }
        if(getVmXMS() != 0) {
            builder.append(" -Xms");
            builder.append(getVmXMS());
            builder.append("m ");
        }
        if(getOtherVMOptions() != null) {
            builder.append(getOtherVMOptions());
        }
        return builder.toString();
    }
}
