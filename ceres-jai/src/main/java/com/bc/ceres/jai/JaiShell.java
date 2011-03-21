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

package com.bc.ceres.jai;

import com.bc.ceres.jai.operator.XmlDescriptor;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterListDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class JaiShell {

    String opt;

    public static void main(String[] args) {

        if (args.length == 0) {
            printUsage();
        }

        boolean interactive = false;
        ArrayList<String> argList = new ArrayList<String>();
        HashMap<String, String> configuration = new HashMap<String, String>(64);
        Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            configuration.put(name, properties.getProperty(name));
        }

        for (String arg : args) {
            if (arg.equals("-i")) {
                interactive = true;
            } else if (arg.startsWith("-D")) {
                String[] tokens = arg.substring(2).split("=", 2);
                configuration.put(tokens[0], tokens.length == 2 ? tokens[1] : "");
            } else if (arg.startsWith("-?")) {
                printOpHelp(arg, 2);
            } else {
                argList.add(arg);
            }
        }

        for (String arg : argList) {
            File file = new File(arg);
            try {
                URI location = createInput(file, configuration);
                XmlDescriptor.create(location, null, null).getRendering();
            } catch (Throwable t) {
                System.out.println("Error: " + t.getMessage());
            }
        }

        if (interactive) {
            System.out.println("Note: The interactive shell is not yet implemented. \n" +
                               "      You can only use the '?' or '?<op>' function.");

            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("\n> ");
                String line;
                try {
                    line = r.readLine();
                    if (line == null) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                line = line.trim();
                if (line.equalsIgnoreCase("exit")) {
                    System.out.println("Bye!");
                    break;
                }
                if (line.startsWith("?")) {
                    printOpHelp(line, 1);
                }
            }
        }
    }

    private static URI createInput(File file, HashMap<String, String> conf) throws IOException {
        String text = readText(file, conf);
        // System.out.println(text);
        File tempFile = File.createTempFile("jai", "xml");
        // todo - I (nf) don't remember, why I need the following?!
        FileWriter writer = new FileWriter(tempFile);
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
        return tempFile.toURI();
    }

    private static String readText(File file, HashMap<String, String> conf) throws IOException {
        return replace(read(file), conf).toString();
    }

    private static StringBuilder read(File file) throws IOException {
        Reader reader = new BufferedReader(new FileReader(file));
        try {
            return readAll(reader);
        } finally {
            reader.close();
        }
    }

    private static StringBuilder readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        while (true) {
            int i = reader.read();
            if (i == -1) {
                break;
            }
            sb.append((char) i);
        }
        return sb;
    }

    private static StringBuilder replace(StringBuilder text, HashMap<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String p = "${" + entry.getKey() + "}";
            while (true) {
                int i = text.indexOf(p);
                if (i == -1) {
                    break;
                }
                text.replace(i, i + p.length(), entry.getValue());
            }
        }
        return text;
    }

    private static void printOpHelp(String s, int i) {
        String name = s.substring(i);
        if (name.equals("")) {
            printOperatorList();
        } else {
            printOperatorHelp(name);
        }
    }

    private static void printOperatorHelp(String name) {
        OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
        OperationDescriptor descriptor = (OperationDescriptor) operationRegistry.getDescriptor(OperationDescriptor.class, name);
        if (descriptor == null) {
            System.out.println("Unknown operation '" + name + "'");
            return;
        }

        String[][] resources = descriptor.getResources(Locale.getDefault());
        String globalName = resources[0][1];
        String description = resources[3][1];

        String[] sourceNames = descriptor.getSourceNames();
        Class[] sourceTypes = descriptor.getSourceClasses("rendered");
        ParameterListDescriptor parameterListDescriptor = descriptor.getParameterListDescriptor("rendered");
        String[] paramNames = parameterListDescriptor.getParamNames();
        Class[] paramTypes = parameterListDescriptor.getParamClasses();

        StringBuilder text = new StringBuilder();
        text.append("Usage: ");
        text.append(globalName);
        text.append('(');
        StringBuilder paramListText = new StringBuilder();
        if (sourceNames != null) {
            for (String sourceName : sourceNames) {
                if (paramListText.length() > 0) {
                    paramListText.append(", ");
                }
                paramListText.append(sourceName);
            }
        }
        if (paramNames != null) {
            for (String paramName : paramNames) {
                if (paramListText.length() > 0) {
                    paramListText.append(", ");
                }
                paramListText.append(paramName);
                Object defaultValue = parameterListDescriptor.getParamDefaultValue(paramName);
                if (defaultValue != ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
                    paramListText.append("=");
                    paramListText.append(format(defaultValue));
                }
            }
        }
        text.append(paramListText);
        text.append(')');
        text.append('\n');
        text.append("Description: ");
        text.append(description);
        text.append('\n');
        text.append("Arguments:\n");
        if (sourceNames != null) {
            for (int i = 0; i < sourceNames.length; i++) {
                String sourceName = sourceNames[i];
                Class sourceType = sourceTypes[i];
                text.append("  ");
                text.append(sourceName);
                text.append(": ");
                text.append("A source.");
                text.append(" (" + sourceType.getName() + ")");
                text.append('\n');
            }
        }

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Class paramType = paramTypes[i];

                text.append("  ");
                text.append(paramName);

                text.append(": ");
                text.append(resources[6 + i][1]);
                text.append(" (" + paramType.getName() + ")");
                text.append('\n');
            }
        }

        System.out.println(text);
    }

    private static String format(Object value) {
        if (value == null) {
            return "null";
        }
        Class<? extends Object> type = value.getClass();
        if (type.isArray()) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(format(Array.get(value, i)));
            }
            builder.append('}');
            return builder.toString();
        } else if (EnumeratedParameter.class.isAssignableFrom(type)) {
            return ((EnumeratedParameter) value).getName();
        } else if (Interpolation.class.isAssignableFrom(type)) {
            if (Interpolation.getInstance(Interpolation.INTERP_NEAREST).equals(value)) {
                return "INTERP_NEAREST";
            } else if (Interpolation.getInstance(Interpolation.INTERP_BILINEAR).equals(value)) {
                return "INTERP_BILINEAR";
            } else if (Interpolation.getInstance(Interpolation.INTERP_BICUBIC).equals(value)) {
                return "INTERP_BICUBIC";
            } else if (Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2).equals(value)) {
                return "INTERP_BICUBIC_2";
            }
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return "\"" + String.valueOf(value) + "\"";
        }
        return String.valueOf(value);
    }

    private static void printUsage() {
        System.out.println("Usage: jai [-?[<op>]] [-i] {-D<param-name>=<param-value>} {<xml-file>}\n");
    }

    private static void printOperatorList() {
        OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
        List<OperationDescriptor> descriptors = (List<OperationDescriptor>) operationRegistry.getDescriptors(OperationDescriptor.class);

        Collections.sort(descriptors, new Comparator<OperationDescriptor>() {
            public int compare(OperationDescriptor descriptor1, OperationDescriptor descriptor2) {
                return descriptor1.getName().compareTo(descriptor2.getName());
            }
        });

        int columnCount = 0;
        for (OperationDescriptor descriptor : descriptors) {
            columnCount = Math.max(columnCount, descriptor.getName().length());
        }

        for (OperationDescriptor descriptor : descriptors) {
            String namePart = descriptor.getName();
            if (namePart.length() < columnCount) {
                namePart += spaces(columnCount - namePart.length());
            }
            String[][] resources = descriptor.getResources(Locale.getDefault());
            String description = resources[3][1];
            System.out.println(namePart + " - " + description);
        }
    }


    private static String spaces(int n) {
        char[] c = new char[n];
        Arrays.fill(c, ' ');
        return new String(c);
    }
}
