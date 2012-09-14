/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.binary;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Attribute;
import org.jdom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Binary database reader
 */
public final class BinaryDBReader {

    private final static int Skip = 0;
    private final static int An = 1;
    private final static int In = 2;
    private final static int B1 = 3;
    private final static int B4 = 4;
    private final static int Fn = 5;
    private final static int B2 = 6;
    private final static int En = 7;
    private final static int B8 = 8;
    private final static int Debug = -1;

    private final Map<String, Object> metaMap = new HashMap<String, Object>(100);
    private final org.jdom.Document xmlDoc;
    private final String recName;
    private final long startPos;

    private final static boolean DEBUG_MODE = false;

    public BinaryDBReader(final org.jdom.Document xmlDoc, final String recName, final long startPos) {
        this.xmlDoc = xmlDoc;
        this.recName = recName;
        this.startPos = startPos;
    }

    public void assignMetadataTo(final MetadataElement elem) {

        final Set<String> keys = metaMap.keySet();                           // The set of keys in the map.
        for (final String key : keys) {
            final Object value = metaMap.get(key);                   // Get the value for that key.
            if (value == null) continue;

            if(value instanceof String) {
                elem.setAttributeString(key, value.toString());
            } else if(value instanceof Integer) {
                elem.setAttributeInt(key, (Integer)value);
            } else if(value instanceof Double) {
                MetadataAttribute attrib = new MetadataAttribute(key, ProductData.TYPE_FLOAT64, 1);
                attrib.getData().setElemDouble((Double)value);
                elem.addAttribute(attrib);
            } else {
                elem.setAttributeString(key, String.valueOf(value));
            }
        }
    }

    public void readRecord(final BinaryFileReader reader) {
        final Element root = xmlDoc.getRootElement();

        if(DEBUG_MODE)
            System.out.print("\nReading "+recName+"\n\n");

        final List children = root.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;

                if(child.getName().equals("struct")) {
                    final Attribute loopAttrib = child.getAttribute("loop");
                    int loop;
                    if(loopAttrib != null) {
                        final String loopName = loopAttrib.getValue();
                        loop = getAttributeInt(loopName);
                    } else {
                        final Attribute nloopAttrib = child.getAttribute("nloop");
                        loop = Integer.parseInt(nloopAttrib.getValue());
                    }                                                                      

                    final List structChildren = child.getChildren();
                    for(int l=1; l <= loop; ++l) {

                        final String suffix = " " + l;
                        for (Object aStructChild : structChildren) {
                            if (aStructChild instanceof Element) {

                                if(DEBUG_MODE) {
                                    DecodeElementDebug(reader, metaMap, (Element) aStructChild, suffix);
                                } else {
                                    DecodeElement(reader, metaMap, (Element) aStructChild, suffix);
                                }
                            }
                        }
                    }
                }

                if(DEBUG_MODE) {
                    DecodeElementDebug(reader, metaMap, child, null);
                } else {
                    DecodeElement(reader, metaMap, child, null);
                }
            }
        }

    }

    private static void DecodeElement(final BinaryFileReader reader, final Map metaMap,
                                      final Element child, final String suffix) {

        String name="";
        try {
            final Attribute nameAttrib = child.getAttribute("name");
            final Attribute typeAttrib = child.getAttribute("type");
            final Attribute numAttrib = child.getAttribute("num");
            if(nameAttrib != null && typeAttrib != null && numAttrib != null) {

                name = nameAttrib.getValue();
                if(suffix != null)
                    name += suffix;
                final int type = Integer.parseInt(typeAttrib.getValue());
                final int num = Integer.parseInt(numAttrib.getValue());

                switch (type) {
                    case Skip: {
                        reader.skipBytes(num); // blank
                        break;
                    } case An: {
                        metaMap.put(name, reader.readAn(num));
                        break;
                    } case In: {
                        metaMap.put(name, (int)reader.readIn(num));
                        break;
                    } case B1: {
                        metaMap.put(name, reader.readB1());
                        break;
                    } case B2: {
                        metaMap.put(name, reader.readB2());
                        break;
                    } case B4: {
                        metaMap.put(name, reader.readB4());
                        break;
                    } case B8: {
                        metaMap.put(name, reader.readB8());
                        break;
                    } case Fn: {
                        metaMap.put(name, reader.readFn(num));
                        break;
                    } case En: {
                        metaMap.put(name, reader.readEn(num));
                        break;
                    } case Debug: {
                        System.out.print(" = ");
                        for(int i=0; i < num; ++i) {
                            final String tmp = reader.readAn(1);
                            if(!tmp.isEmpty() && !tmp.equals(" "))
                                System.out.print(tmp);
                        }
                        System.out.println();
                        break;
                    } default: {
                        throw new IllegalBinaryFormatException("Unknown type " + type, reader.getCurrentPos());
                    }
                }
            }

        } catch(Exception e) {
            System.out.println(' ' +e.toString() + ':' +e.getCause().toString() + " for "+ name);
        }
    }

    private void DecodeElementDebug(final BinaryFileReader reader, final Map metaMap,
                                      final Element child, final String suffix) {

        String name="";
        try {
            final Attribute nameAttrib = child.getAttribute("name");
            final Attribute typeAttrib = child.getAttribute("type");
            final Attribute numAttrib = child.getAttribute("num");
            if(nameAttrib != null && typeAttrib != null && numAttrib != null) {

                name = nameAttrib.getValue();
                if(suffix != null)
                    name += suffix;
                final int type = Integer.parseInt(typeAttrib.getValue());
                final int num = Integer.parseInt(numAttrib.getValue());

                System.out.print(" " + reader.getCurrentPos() + ' ' + (reader.getCurrentPos()-startPos+1) +
                                 ' ' + name + ' ' + type + ' ' + num);

                switch (type) {
                    case Skip: {
                        reader.skipBytes(num); // blank
                        break;
                    } case An: {

                        final String tmp = reader.readAn(num);
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case In: {

                        final int tmp = (int)reader.readIn(num);
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case B1: {

                        final int tmp = reader.readB1();
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case B2: {

                        final int tmp = reader.readB2();
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case B4: {

                        final int tmp = reader.readB4();
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case B8: {

                        final long tmp = reader.readB8();
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case Fn: {

                        double tmp = reader.readFn(num);
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case En: {

                        double tmp = reader.readEn(num);
                        System.out.print(" = " + tmp);
                        metaMap.put(name, tmp);
                        break;
                    } case Debug: {

                        System.out.print(" = ");
                        for(int i=0; i < num; ++i) {
                            final String tmp = reader.readAn(1);
                            if(!tmp.isEmpty() && !tmp.equals(" "))
                                System.out.print(tmp);
                        }
                        System.out.println();
                        break;
                    } default: {
                        throw new IllegalBinaryFormatException("Unknown type " + type, reader.getCurrentPos());
                    }
                }
                System.out.println();
            }

        } catch(Exception e) {
            System.out.println(' ' +e.toString() + ':' +e.getCause().toString() + " for "+ name);

            //throw new IllegalBinaryFormatException(e.toString(), reader.getCurrentPos());
        }
    }

    private Object get(final String name) {
        final Object obj = metaMap.get(name);
        if(obj == null && DEBUG_MODE) {
            System.out.println("metadata "+name+" is null");
        }
        return obj;
    }

    public final String getAttributeString(final String name) {
        return (String) get(name);
    }
    
    public final Integer getAttributeInt(final String name) {
        return (Integer) get(name);
    }

    public final Double getAttributeDouble(final String name) {
        return (Double) get(name);
    }

    public final void set(final String name, final Object o) {
        metaMap.put(name, o);
    }

    /**
     * Read in the definition file
     * @param mission sub folder
     * @param fileName definition file
     * @return xml document
     */
    public static org.jdom.Document loadDefinitionFile(final String mission, final String fileName) {
        try {
            final File defFile = getResFile(mission, fileName);
            return XMLSupport.LoadXML(defFile.getAbsolutePath());
        } catch(Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    private static File getResFile(final String mission, final String fileName) {
        final String homeUrl = ResourceUtils.findHomeFolder().getAbsolutePath();
        final String path = homeUrl + File.separator + "res" + File.separator + "ceos_db" +
                File.separator + mission + File.separator + fileName;
        return new File(path);
    }
}
