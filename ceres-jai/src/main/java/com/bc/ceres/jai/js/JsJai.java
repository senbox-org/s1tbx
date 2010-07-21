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

package com.bc.ceres.jai.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.PlanarImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class JsJai extends ScriptableObject {

    public JsJai() {
        construct();
    }

    public JsJai(Scriptable scope, Scriptable prototype) {
        super(scope, prototype);
        construct();
    }

    public String getClassName() {
        return "jai";
    }

    @Override
    public Object getDefaultValue(Class aClass) {
        if (aClass == String.class) {
            return "[" + getClassName() + "]";
        } else if (aClass == Boolean.class) {
            return false;
        } else if (aClass == Number.class) {
            return 0;
        } else if (aClass == Scriptable.class) {
            return this;
        } else {
            return null;
        }
    }

    private void construct() {
        final OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
        final List<OperationDescriptor> operationDescriptors = (List<OperationDescriptor>) operationRegistry.getDescriptors(OperationDescriptor.class);
        for (OperationDescriptor operationDescriptor : operationDescriptors) {
            final JsJaiFunction jaiFunction = new JsJaiFunction(operationDescriptor);
            defineProperty(jaiFunction.getClassName(), jaiFunction, ScriptableObject.READONLY);
            final Field[] fields = operationDescriptor.getClass().getFields();
            for (Field field : fields) {
                final int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    try {
                        defineProperty(field.getName(), field.get(operationDescriptor), READONLY);
                    } catch (IllegalAccessException e) {
                        System.out.println("Error: operationDescriptor=" + operationDescriptor + ", field=" + field + ", e=" + e);
                    }
                }
            }
        }
        defineProperty("INTERP_NEAREST", Interpolation.getInstance(Interpolation.INTERP_NEAREST), READONLY);
        defineProperty("INTERP_BILINEAR", Interpolation.getInstance(Interpolation.INTERP_BILINEAR), READONLY);
        defineProperty("INTERP_BICUBIC", Interpolation.getInstance(Interpolation.INTERP_BICUBIC), READONLY);
        defineProperty("INTERP_BICUBIC_2", Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2), READONLY);
        defineFunctionProperties(new String[]{"show", "help"},
                                 JsJai.class,
                                 ScriptableObject.DONTENUM);


    }

    static int frameId = 0;
    static HashMap<Integer, JFrame> frames = new HashMap<Integer, JFrame>();

    public static int show(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        final RenderedImage renderedImage = args.length > 0 ?  (RenderedImage) Context.jsToJava(args[0], RenderedImage.class) : null;
        int frameId = args.length > 1 ? (Integer) Context.jsToJava(args[1], Integer.class) : -1;

        final Image image;
        if (renderedImage instanceof PlanarImage) {
            image = ((PlanarImage) renderedImage).getAsBufferedImage();
        } else if (renderedImage instanceof Image) {
            image = (Image) renderedImage;
        } else {
            image = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
        }

        JFrame frame = frames.get(frameId);
        if (frame != null) {
            frame.getContentPane().removeAll();
        } else {
            frameId = JsJai.frameId++;
            frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frames.put(frameId, frame);
        }

        frame.setTitle("Image #" + frameId);
        frame.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(image))));
        frame.pack();
        frame.setVisible(true);

        return frameId;
    }

    public static void help(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length == 0) {
            final Object[] objects = thisObj.getIds();
            for (Object object : objects) {
                System.out.println("jai." + object + " = " + thisObj.get(object.toString(), thisObj));
            }
            printOperatorList();
        } else {
            for (Object arg : args) {
                printOperatorUsage(Context.toString(arg));
            }
        }
    }

    public static void printOperatorList() {
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

    private static void printOperatorUsage(String name) {
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


    private static String spaces(int n) {
        char[] c = new char[n];
        Arrays.fill(c, ' ');
        return new String(c);
    }
}