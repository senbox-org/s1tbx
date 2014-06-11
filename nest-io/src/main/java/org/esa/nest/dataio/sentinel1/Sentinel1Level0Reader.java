/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.sentinel1;

import com.bc.jexp.impl.AbstractFunction;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dataio.FileImageInputStreamExtImpl;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.IllegalBinaryFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//import org.w3c.dom.Node;
/**
 * TBD
 */
public class Sentinel1Level0Reader {

    private Product product = null;

    private final String SUPPORT_FOLDER_NAME = "support";
    private final String ANNOT_SCHEMA_FILENAME = "s1-level-0-annot.xsd";
    private final String INDEX_SCHEMA_FILENAME = "s1-level-0-index.xsd";

    // Prefixes used in filename
    private final String ANNOT_PREFIX = "-annot";
    private final String INDEX_PREFIX = "-index";

    // Content of attribute "name" in tag "xs:complexType"
    private final String ANNOT_RECORD_NAME = "annotRecordType";
    private final String INDEX_RECORD_NAME = "block";

    // Tag names
    private final String SIMPLE_TYPE_TAG_NAME = "xs:simpleType";
    private final String COMPLEX_TYPE_TAG_NAME = "xs:complexType";
    private final String SEQUENCE_TAG_NAME = "xs:sequence";
    private final String ELEMENT_TAG_NAME = "xs:element";
    private final String RESTRICTION_TAG_NAME = "xs:restriction";
    private final String ANNOTATION_TAG_NAME = "xs:annotation";
    private final String APPINFO_TAG_NAME = "xs:appinfo";
    private final String BLOCK_TAG_NAME = "sdf:block";
    private final String LENGTH_TAG_NAME = "sdf:length";
    private final String OCCURRENCE_TAG_NAME = "sdf:occurrence";

    // Base types
    private final String BOOLEAN_TAG_NAME = "xs:boolean"; // 1 byte
    private final String UNSIGNED_BYTE_TAG_NAME = "xs:unsignedByte"; // 1 byte
    private final String UNSIGNED_SHORT_TAG_NAME = "xs:unsignedShort"; // 2 bytes
    private final String UNSIGNED_INT_TAG_NAME = "xs:unsignedInt"; // 4 bytes
    private final String UNSIGNED_LONG_TAG_NAME = "xs:unsignedLong"; // 8 bytes
    private final String DOUBLE_TAG_NAME = "xs:double"; // 8 bytes

    private final List<String> baseTypeTagNameList = Arrays.asList(BOOLEAN_TAG_NAME,
                                                                    UNSIGNED_BYTE_TAG_NAME,
                                                                    UNSIGNED_SHORT_TAG_NAME,
                                                                    UNSIGNED_INT_TAG_NAME,
                                                                    UNSIGNED_LONG_TAG_NAME,
                                                                    DOUBLE_TAG_NAME);
    private final String BIT_BASE_TYPE = "BIT_BASE_TYPE";

    // Attributes in tags
    private final String NAME_ATTRIBUTE = "name";
    private final String TYPE_ATTRIBUTE = "type";
    private final String BASE_ATTRIBUTE = "base";
    private final String UNIT_ATTRIBUTE = "unit";

    private final class DataElement {

        private final String name; // content of attribute "name"
        private final String type; // content of attribute "type"

        private final String baseType; // must be one of what are in baseTypeTagNameList or BIT_BASE_TYPE

        private final int numBytes; // number of bytes in this data element (should agree with baseType);
                                    // if baseType == BIT_BASE_TYPE, then numBytes is number of bits

        private final int startBit; // applicable only if baseType == BIT_BASE_TYPE

        private final int numOccurrences; // number of occurrences of this data element

        DataElement(final String name, final String type, final String baseType, final int numBytes, final int startBit, final int numOccurrences) {

            this.name = name;
            this.type = type;

            this.baseType = baseType;
            this.numBytes = numBytes;
            this.startBit = startBit;
            this.numOccurrences = numOccurrences;
        }

        private void dump() {
            System.out.println(" name = " + name + "; type = " + type + "; baseType = " + baseType + "; numBytes = " + numBytes + "; startBit = " + startBit + "; numOccurrences = " + numOccurrences);
        }
    }

    private ArrayList<DataElement> annotElemList = new ArrayList<>();
    private ArrayList<DataElement> indexElemList = new ArrayList<>();

    private class DataComponent {

        private final BinaryFileReader reader;
        private final ArrayList<DataElement> elemList;
        private final MetadataElement parentMetadataElem;
        private final long numRecords;

        DataComponent(final BinaryFileReader reader, final ArrayList<DataElement> elemList, final MetadataElement parentMetadataElem, final long numRecords) {

            this.reader = reader;
            this.elemList = elemList;
            this.parentMetadataElem = parentMetadataElem;
            this.numRecords = numRecords;
        }
    }

    private ArrayList<DataComponent> dataComponents = new ArrayList<>();

    public Sentinel1Level0Reader(Product product) {

        this.product = product;

        readXMLSchema(buildSchemaFilename(ANNOT_SCHEMA_FILENAME), ANNOT_RECORD_NAME, annotElemList);
        readXMLSchema(buildSchemaFilename(INDEX_SCHEMA_FILENAME), INDEX_RECORD_NAME, indexElemList);

        //  Metadata > Original_Product_Metadata > XFDU > dataObjectSection >
        //       dataObject > byteStream > fileLocation

        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement dataObjectSection = root.getElement("Original_Product_Metadata").getElement("XFDU").getElement("dataObjectSection");

        final MetadataElement annotElem = new MetadataElement("Annotation Data Components");
        root.addElement(annotElem);

        final MetadataElement indexElem = new MetadataElement("Index Data Components");
        root.addElement(indexElem);

        final MetadataElement measurementDataElem = new MetadataElement("Measurement Data Components");
        root.addElement(measurementDataElem);

        int numElem = dataObjectSection.getNumElements();

        for (int i = 0; i < numElem; i++) {

            MetadataElement elem = dataObjectSection.getElementAt(i).getElement("byteStream");
            String dataFilename = elem.getElement("fileLocation").getAttributeString("href");

            MetadataElement recordElem = dataFilename.contains(INDEX_PREFIX) ? new MetadataElement("blocks") : new MetadataElement("records");

            //System.out.println("Sentinel1Level0Reader: " + dataFilename);

            MetadataElement componentElem;

            if (dataFilename.contains(ANNOT_PREFIX)) {

                componentElem = new MetadataElement(extractPolarization(dataFilename) + "annotation");
                annotElem.addElement(componentElem);

            } else if (dataFilename.contains(INDEX_PREFIX)) {

                componentElem = new MetadataElement(extractPolarization(dataFilename) + "index");
                indexElem.addElement(componentElem);

            } else {

                componentElem = new MetadataElement(extractPolarization(dataFilename) + "measurement_data");
                measurementDataElem.addElement(componentElem);
            }

            final MetadataAttribute nameAttr = new MetadataAttribute("filename", ProductData.TYPE_ASCII);
            componentElem.addAttribute(nameAttr);
            nameAttr.getData().setElems(dataFilename);

            final MetadataAttribute numRecsAttr = new MetadataAttribute("number of " + recordElem.getName(), ProductData.TYPE_UINT32);
            componentElem.addAttribute(numRecsAttr);

            componentElem.addElement(recordElem);

            if (dataFilename.contains(ANNOT_PREFIX) || dataFilename.contains(INDEX_PREFIX)) {

                final long numRecs = createBinaryReader(dataFilename, recordElem);
                numRecsAttr.getData().setElemUInt(numRecs);
            }


            // TODO handle measurement data
        }

    }

    private long createBinaryReader(final String binDataFilename, MetadataElement metadataElement) {

        File binDataFile = new File(product.getFileLocation().getAbsolutePath() + binDataFilename);

        long numRecs = 0;

        try {
            ImageInputStream imageInputStream = FileImageInputStreamExtImpl.createInputStream(binDataFile);

            final BinaryFileReader binaryReader = new BinaryFileReader(imageInputStream);

            // According to Product Specs, binary data is stored in Big Endian format.
            binaryReader.setByteOrder(ByteOrder.BIG_ENDIAN);

            DataComponent dataComponent = null;

            final long filesize = binDataFile.length(); // bytes

            if (binDataFilename.contains(ANNOT_PREFIX)) {

                numRecs = filesize/getTotalNumberOfBytes(annotElemList);
                dataComponent = new DataComponent(binaryReader, annotElemList, metadataElement, numRecs);

            } else if (binDataFilename.contains(INDEX_PREFIX)) {

                numRecs = filesize/getTotalNumberOfBytes(indexElemList);
                dataComponent = new DataComponent(binaryReader, indexElemList, metadataElement, numRecs);
            }

            dataComponents.add(dataComponent);

        } catch (IOException e) {

            System.out.println("Sentinel1Level0Reader.createBinaryReader: IOException " + e.getMessage());
        }

        return numRecs;
    }

    private String buildSchemaFilename(final String schemaName) {

        return product.getFileLocation().getAbsolutePath() + "\\" + SUPPORT_FOLDER_NAME + "\\" + schemaName;
    }

    private void readXMLSchema(final String filename, final String recordName, ArrayList<DataElement> elemList) {

        final File xmlFile = new File(filename);

        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(xmlFile);

            if (doc == null) {

                System.out.println("Sentinel1Level0Reader.readXMLSchema: ERROR failed to create Document for XML schema");
                return;
            }

            doc.getDocumentElement().normalize();

            org.w3c.dom.Node recNode;

            // Look for a complexType tag in the XML file with name attribute == recordName
            // Inside this tag, there should be a sequence tag containing all the elements in one record of the
            // data component.

            recNode = getNodeFromDocument(doc, COMPLEX_TYPE_TAG_NAME, recordName);

            if (recNode == null) {
                System.out.println("Sentinel1Level0Reader.readXMLSchema: ERROR failed to find " + COMPLEX_TYPE_TAG_NAME + " in schema");
                return;
            }

            org.w3c.dom.Node sequenceNode = getNodeFromNode(recNode, SEQUENCE_TAG_NAME);

            if (sequenceNode == null) {
                System.out.println("Sentinel1Level0Reader.readXMLSchema: ERROR failed to find " + SEQUENCE_TAG_NAME + " in schema");
                return;
            }

            getElementsInRecord(sequenceNode, elemList);

            //dumpElemList(recordName, elemList);

        } catch (IOException e) {

            System.out.println("Sentinel1Level0Reader.readXMLSchema: IOException " + e.getMessage());

        } catch (ParserConfigurationException e) {

            System.out.println("Sentinel1Level0Reader.readXMLSchema: ParserConfigurationException " + e.getMessage());

        } catch (SAXException e) {

            System.out.println("Sentinel1Level0Reader.readXMLSchema: SAXException " + e.getMessage());
        }
    }

    private org.w3c.dom.Node getNodeFromDocument(final Document doc, final String nodeType, final String nodeName) {

        // Will look for tag with name == nodeType whose attribute name == nodeName.

        // This will return a list of all tags with name == nodeType
        NodeList nodeList = doc.getElementsByTagName(nodeType);

        org.w3c.dom.Node recNode = null;

        // Then we loop each tag with the same tag name which is nodeType
        for (int i = 0; i < nodeList.getLength(); i++) {

            org.w3c.dom.Node node = nodeList.item(i);

            //System.out.println(" Record name " + node.getNodeName());

            // Get all the attributes in the tag
            NamedNodeMap attr = node.getAttributes();

            // Loop through each attribute to look for NAME_ATTRIBUTE and check if it is equal to nodeName
            for (int j = 0; j < attr.getLength(); j++) {

                if (attr.item(j).getNodeName().equals(NAME_ATTRIBUTE) && attr.item(j).getTextContent().contains(nodeName)) {

                    //System.out.println(" found : " + attr.item(j).getTextContent());

                    if (recNode == null) {
                        recNode = node;
                    } else {
                        System.out.println("Sentinel1Level0Reader.getNodeFromDocument: WARNING more than one " + nodeName + " of type " + nodeType + " in " + doc.getDocumentURI());
                    }
                }
            }
        }

        return recNode;
    }

    private org.w3c.dom.Node getNodeFromNode(final org.w3c.dom.Node parentNode, final String nodeName) {

        // Will look for node with tag name == nodeName in parentNode

        org.w3c.dom.Node node = null;

        NodeList childNodes = parentNode.getChildNodes();

        // Loop through all child nodes and look for the child whose name == nodeName
        for (int i = 0; i < childNodes.getLength(); i++) {

            org.w3c.dom.Node child = childNodes.item(i);

            //System.out.println(" child name " + child.getNodeName());

            if (child.getNodeName().equals(nodeName)) {

                if (node == null) {
                    //System.out.println("Sentinel1Level0Reader: found " + nodeName + " in " + parentNode.getNodeName());
                    node = child;
                } else {
                    System.out.println("Sentinel1Level0Reader.getNodeFromNode: WARNING more than one " + nodeName + " in " + parentNode.getNodeName());
                }
            }
        }

        return node;
    }

    private org.w3c.dom.Node getNodeFromNode(final org.w3c.dom.Node parentNode, final String[] nodeNames) {

        // Will look for node at the end of a branch defined by a sequence of nodes.

        org.w3c.dom.Node node = parentNode;

        for (String n : nodeNames) {

            node = getNodeFromNode(node, n);
        }

        return node;
    }

    private org.w3c.dom.Node getAttributeFromNode(final org.w3c.dom.Node node, final String attrName) {

        // Will look for attribute called attrName in the given node

        NamedNodeMap attr = node.getAttributes();

        org.w3c.dom.Node attrNode = null;

        for (int j = 0; j < attr.getLength(); j++) {

            //System.out.println(" attribute = " + attr.item(j).getNodeName());

            if (attr.item(j).getNodeName().equals(attrName)) {
                if (attrNode == null) {
                    attrNode = attr.item(j);
                } else {
                    // Should not be possible
                    System.out.println("Sentinel1Level0Reader.getAttributeFromNode: WARNING more than one " + attrName + " in " + node.getNodeName());
                }
            }
        }

        return attrNode;
    }

    private boolean isBaseType(final org.w3c.dom.Node node, final String type, final int[] numBytes, final int[] numOccurrence) {

        if (baseTypeTagNameList.contains(type)) {

            numBytes[0] = -1;
            numOccurrence[0] = 1;

            String[] nodeNames = {ANNOTATION_TAG_NAME, APPINFO_TAG_NAME, BLOCK_TAG_NAME};

            org.w3c.dom.Node blockNode = getNodeFromNode(node, nodeNames);

            if (blockNode == null) {
                System.out.println("Sentinel1Level0Reader.getOccurrencesAndLength: no block in " + node.getNodeName());
                return true;
            }

            org.w3c.dom.Node occurrenceNode = getNodeFromNode(blockNode, OCCURRENCE_TAG_NAME);

            if (occurrenceNode != null) {
                numOccurrence[0] = Integer.parseInt(occurrenceNode.getTextContent());
            } else {
                System.out.println("Sentinel1Level0Reader.getOccurrencesAndLength: no occurrence in " + node.getNodeName());
            }

            org.w3c.dom.Node lengthNode = getNodeFromNode(blockNode, LENGTH_TAG_NAME);

            if (lengthNode != null) {
                numBytes[0] = Integer.parseInt(lengthNode.getTextContent());
            } else {
                System.out.println("Sentinel1Level0Reader.getOccurrencesAndLength: no length in " + node.getNodeName());
            }

            return true;
        }

        return false;
    }

    private String getBaseType(final Document doc, String type, final int[] numBytes) {

        // Returns base type and output numBytes

        String baseType = "";
        numBytes[0] = -1;

        org.w3c.dom.Node typeNode;

        // Look for simpleType whose name == type in schema
        typeNode = getNodeFromDocument(doc, SIMPLE_TYPE_TAG_NAME, type);

        if (typeNode != null) {

            org.w3c.dom.Node restrictionNode = getNodeFromNode(typeNode, RESTRICTION_TAG_NAME);

            if (restrictionNode == null) {
                System.out.println("Sentinel1Level0Reader.getBaseType: failed to find " + RESTRICTION_TAG_NAME + " in " + typeNode.getNodeName());
                return baseType;
            }

            org.w3c.dom.Node baseTypeNode = getAttributeFromNode(restrictionNode, BASE_ATTRIBUTE);

            if (baseTypeNode == null) {
                System.out.println("Sentinel1Level0Reader.getBaseType: failed to find " + BASE_ATTRIBUTE + " in " + restrictionNode.getNodeName());
                return baseType;
            }

            baseType = baseTypeNode.getTextContent();

            String[] nodeNames = {ANNOTATION_TAG_NAME, APPINFO_TAG_NAME, BLOCK_TAG_NAME, LENGTH_TAG_NAME};

            org.w3c.dom.Node lengthNode = getNodeFromNode(typeNode, nodeNames);

            if (lengthNode == null) {
                System.out.println("Sentinel1Level0Reader.getBaseType: failed to find " + LENGTH_TAG_NAME + " in branch of " + typeNode.getNodeName());
                return baseType;
            }

            numBytes[0] = Integer.parseInt(lengthNode.getTextContent());

            org.w3c.dom.Node unitNode = getAttributeFromNode(lengthNode, UNIT_ATTRIBUTE);

            // It is valid for unitNode to be null in which case unit of length is bytes
            if (unitNode != null && unitNode.getTextContent().equals("bit")) {
                baseType = BIT_BASE_TYPE;
            }

        } else {

            System.out.println("Sentinel1Level0Reader.getBaseType: ERROR missing " + SIMPLE_TYPE_TAG_NAME + " in " + doc.getDocumentURI());
        }

        return baseType;
    }

    private void getElementsInRecord(final org.w3c.dom.Node sequenceNode, final ArrayList<DataElement> elemList) {

        NodeList childNodes = sequenceNode.getChildNodes();

        DataElement prevDataElem = null;

        for (int i = 0; i < childNodes.getLength(); i++) {

            org.w3c.dom.Node child = childNodes.item(i);

            //System.out.println(" child name " + child.getNodeName());

            if (child.getNodeName().equals(ELEMENT_TAG_NAME)) {

                String elemName = "";
                String elemType = "";
                String elemBaseType = "";
                int[] elemNumOccurrences = new int[1];
                int[] elemNumBytes = new int[1];
                int startBit = -1;

                NamedNodeMap attr = child.getAttributes();

                for (int j = 0; j < attr.getLength(); j++) {

                    //System.out.println(" attribute = " + attr.item(j).getNodeName());

                    if (attr.item(j).getNodeName().equals(NAME_ATTRIBUTE)) {
                        elemName = attr.item(j).getTextContent();
                    } else if (attr.item(j).getNodeName().equals(TYPE_ATTRIBUTE)) {
                        elemType = attr.item(j).getTextContent();
                    }
                }

                if (isBaseType(child, elemType, elemNumBytes, elemNumOccurrences)) {
                    elemBaseType = elemType;
                } else {
                    elemNumOccurrences[0] = 1;
                    elemBaseType = getBaseType(sequenceNode.getOwnerDocument(), stripNamespace(elemType), elemNumBytes);
                }

                if (elemBaseType.equals(BIT_BASE_TYPE)) {
                    if (prevDataElem == null) {
                        startBit = 0;
                    } else if (prevDataElem.baseType.equals(BIT_BASE_TYPE)) {
                        final int prevEndBit = prevDataElem.startBit + prevDataElem.numBytes - 1;
                        if (prevEndBit >= 7) {
                            startBit = 0;
                        } else {
                            startBit = prevEndBit + 1;
                        }
                    } else {
                        startBit = 0;
                    }
                }

                DataElement dataElement = new DataElement(elemName, elemType, elemBaseType, elemNumBytes[0], startBit, elemNumOccurrences[0]);
                elemList.add(dataElement);
                prevDataElem = dataElement;
            }
        }
    }

    private String stripNamespace(final String name) {

        return name.substring(name.lastIndexOf(":")+1);
    }

    private void dumpElemList(final String listName, final ArrayList<DataElement> elemList) {

        System.out.println("Start of " + listName);
        for (DataElement dataElement : elemList) {
            dataElement.dump();
        }
        System.out.println("End of " + listName);
    }

    public void readData() {

        //System.out.println("Sentinel1Level0Reader.readData: called");

        for (DataComponent d : dataComponents) {

            //System.out.println("Sentinel1Level0Reader.readData: read one data component");
            readBinaryData(d);
        }
    }

    private byte readOneBinaryElement(final BinaryFileReader reader,
                                      final DataElement elem,
                                      final DataElement prevElem,
                                      final byte prevByte,
                                      MetadataElement parentMetadataElem) {

        //System.out.println("Sentinel1Level0Reader.readOneBinaryElement: read " + elem.name + " " + elem.baseType);

        byte lastByteRead = 0;

        try {
            switch (elem.baseType) {
                case BOOLEAN_TAG_NAME:
                case UNSIGNED_BYTE_TAG_NAME: {
                    final int val = reader.readUB1();
                    //System.out.println(elem.name + " = " + val + "; " + elem.baseType + " (unsigned byte); pos = " + reader.getCurrentPos());
                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_UINT8);
                    ProductData productData = attr.getData();
                    productData.setElemInt(val);
                    parentMetadataElem.addAttribute(attr);
                }
                    break;
                case UNSIGNED_SHORT_TAG_NAME: {
                    final int val = reader.readUB2();
                    //System.out.println(elem.name + " = " + val + "; " + elem.baseType + " (unsigned short); pos = " + reader.getCurrentPos());
                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_UINT16);
                    ProductData productData = attr.getData();
                    productData.setElemInt(val);
                    parentMetadataElem.addAttribute(attr);
                }
                    break;
                case UNSIGNED_INT_TAG_NAME: {
                    final long val = getUnsignedInt(reader.readB4());
                    //System.out.println(elem.name + " = " + val + "; " + elem.baseType + " (unsigned int); pos = " + reader.getCurrentPos());
                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_UINT32);
                    ProductData productData = attr.getData();
                    productData.setElemUInt(val);
                    parentMetadataElem.addAttribute(attr);
                }
                    break;
                case UNSIGNED_LONG_TAG_NAME: {
                    final byte[] bytes = new byte[8];
                    reader.read(bytes);
                    final BigInteger val = new BigInteger(bytes);
                    final String valStr = String.valueOf(val);
                    //System.out.println(elem.name + " = " + val + "; " + elem.baseType + " (unsigned long);  pos = " + reader.getCurrentPos());
                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_ASCII);
                    ProductData productData = attr.getData();
                    productData.setElems(valStr);
                    parentMetadataElem.addAttribute(attr);
                    // To get back the unsigned long value, one can get the String back and create a BigInteger with it.
                    // Constructor of BigInteger takes a String.
                }
                    break;
                case DOUBLE_TAG_NAME: {
                    final double val = (double)reader.readB8();
                    //System.out.println(elem.name + " = " + val + "; " + elem.baseType + " (double); pos" + reader.getCurrentPos());
                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_FLOAT64);
                    ProductData productData = attr.getData();
                    productData.setElemDouble(val);
                    parentMetadataElem.addAttribute(attr);
                }
                    break;
                case BIT_BASE_TYPE: {
                    if (prevElem == null || elem.startBit == 0) {
                        final byte[] oneByte = new byte[1];
                        reader.read(oneByte);
                        lastByteRead = oneByte[0];
                        //System.out.println(elem.name + " = "  + Integer.toBinaryString(getInteger(lastByteRead)) + "; " + elem.baseType + " (unsigned int); pos = " + reader.getCurrentPos());
                    } else {
                        lastByteRead = prevByte;
                    }

                    final byte val = extract(lastByteRead, elem.startBit, elem.numBytes);

                    MetadataAttribute attr = new MetadataAttribute(elem.name, ProductData.TYPE_UINT8);
                    ProductData productData = attr.getData();
                    productData.setElemInt(getInteger(val));
                    parentMetadataElem.addAttribute(attr);
                }
                    break;
                default: {
                    System.out.println("Sentinel1Level0Reader.readOneBinaryElement: ERROR Unknown baseType = " + elem.baseType);
                }
                    break;
            }

        } catch (IOException e) {

            System.out.println("Sentinel1Level0Reader.readOneBinaryElement: IOException " + e.getMessage());

        } catch (IllegalBinaryFormatException e) {

            System.out.println("Sentinel1Level0Reader.readOneBinaryElement: IllegalBinaryFormatException " + e.getMessage());
        }

        return lastByteRead;
    }

    private void readBinaryData(DataComponent dataComponent) {

        final BinaryFileReader reader = dataComponent.reader;
        final ArrayList<DataElement> elemList = dataComponent.elemList;
        final MetadataElement parentMetadataElem = dataComponent.parentMetadataElem;

        for (long i = 0; i < dataComponent.numRecords; i++) {

            final String parentName = parentMetadataElem.getName();
            final MetadataElement recMetaElem = new MetadataElement(parentName.substring(0, parentName.length()-1) + i);
            parentMetadataElem.addElement(recMetaElem);
            /*
            try {
                System.out.println("readBinaryData: file pos = " + reader.getCurrentPos());
            } catch (IOException e) {
                System.out.println("readBinaryData: file pos = ERROR " + e.getMessage());
            }
            */
            DataElement prevDataElem = null;
            byte prevByte = 0;

            for (DataElement elem : elemList) {

                for (int j = 0; j < elem.numOccurrences; j++) {
                    prevByte = readOneBinaryElement(reader, elem, prevDataElem, prevByte, recMetaElem);
                    prevDataElem = elem;
                }
            }
        }
    }

    private static long getUnsignedInt(final int x) {
        return x & 0x00000000ffffffffL;
    }

    private static int getInteger(final byte b) {
        return b & 0xFF;
    }

    private static byte extract(final byte b, final int startBit, final int numBits) {

        // Assume int is 32-bit, byte is 8-bit
        int result = getInteger(b) << startBit + 24;
        result = result >>> (32 - numBits);

        return (byte)result;
    }

    private static String extractPolarization(String filename) {

        final int idx = filename.lastIndexOf("raw")+6;

        final String pp = filename.substring(idx, idx+2);

        if (pp.equals("hh") || pp.equals("hv") || pp.equals("vv") || pp.equals("vh")) {
            return pp + "_";
        } else {
            return "";
        }
    }

    private int getTotalNumberOfBytes(ArrayList<DataElement> elemList) {

        int total = 0;

        for (DataElement elem : elemList) {

            if (elem.baseType != BIT_BASE_TYPE) {
                total += (elem.numBytes * elem.numOccurrences);
            } else if (elem.startBit == 0) {
                total += 1;
            }
        }

        return total;
    }
}
