/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.binio.binx;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.core.Assert;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * Utility class used to read BinX schema files.
 * See the <a href="http://www.edikt.org/binx/">BinX Project</a>.
 * <p>
 * This class is not thread-safe.
 */
public class BinX {

    static final String ANONYMOUS_COMPOUND_PREFIX = "AnonymousCompound@";
    static final String ARRAY_VARIABLE_PREFIX = "ArrayVariable@";
    static final String DEFAULT_ELEMENT_COUNT_POSTFIX = "_Counter";

    private final Map<String, String> parameters;
    private final Map<String, Type> definitions;
    private final Map<String, String> varNameMap;
    private final Set<String> inlinedStructs;

    private String elementCountPostfix;

    private boolean singleDatasetStructInlined;
    private boolean arrayVariableInlined;
    private Map<String, SimpleType> primitiveTypes;
    private Namespace namespace;
    private static int anonymousCompoundId = 0;

    public BinX() {
        parameters = new HashMap<String, String>();
        definitions = new HashMap<String, Type>();
        varNameMap = new HashMap<String, String>();
        inlinedStructs = new HashSet<String>();

        primitiveTypes = new HashMap<String, SimpleType>();
        primitiveTypes.put("byte-8", SimpleType.BYTE);
        primitiveTypes.put("unsignedByte-8", SimpleType.UBYTE);
        primitiveTypes.put("short-16", SimpleType.SHORT);
        primitiveTypes.put("unsignedShort-16", SimpleType.USHORT);
        primitiveTypes.put("integer-32", SimpleType.INT);
        primitiveTypes.put("unsignedInteger-32", SimpleType.UINT);
        primitiveTypes.put("long-64", SimpleType.LONG);
        primitiveTypes.put("unsignedLong-64", SimpleType.ULONG);
        primitiveTypes.put("float-32", SimpleType.FLOAT);
        primitiveTypes.put("double-64", SimpleType.DOUBLE);

        elementCountPostfix = DEFAULT_ELEMENT_COUNT_POSTFIX;
        singleDatasetStructInlined = false;
        arrayVariableInlined = false;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String setParameter(String name, String value) {
        if (value == null) {
            return parameters.remove(name);
        }
        return parameters.put(name, value);
    }

    public Type getDefinition(String name) {
        return definitions.get(name);
    }

    public Type setDefinition(String name, Type value) {
        if (value == null) {
            return definitions.remove(name);
        }
        return definitions.put(name, value);
    }

    public String setVarNameMapping(String sourceName, String targetName) {
        if (targetName == null) {
            return varNameMap.remove(sourceName);
        }

        return varNameMap.put(sourceName, targetName);
    }

    public void setVarNameMappings(Properties properties) {
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    final String sourceName = (String) entry.getKey();
                    final String targetName = (String) entry.getValue();

                    setVarNameMapping(sourceName, targetName);
                }
            }
        }
    }

    public boolean setTypeMembersInlined(String typeName, boolean b) {
        if (!b) {
            return inlinedStructs.remove(typeName);
        }

        return inlinedStructs.add(typeName);
    }

    public void setTypeMembersInlined(Properties properties) {
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() instanceof String) {
                    final String typeName = (String) entry.getKey();

                    setTypeMembersInlined(typeName, "true".equals(entry.getValue()));
                }
            }
        }
    }

    public String getElementCountPostfix() {
        return elementCountPostfix;
    }

    public void setElementCountPostfix(String elementCountPostfix) {
        Assert.notNull(elementCountPostfix, "elementCountPostfix");
        this.elementCountPostfix = elementCountPostfix;
    }

    public boolean isSingleDatasetStructInlined() {
        return singleDatasetStructInlined;
    }

    public void setSingleDatasetStructInlined(boolean singleDatasetStructInlined) {
        this.singleDatasetStructInlined = singleDatasetStructInlined;
    }

    public boolean isArrayVariableInlined() {
        return arrayVariableInlined;
    }

    public void setArrayVariableInlined(boolean arrayVariableInlined) {
        this.arrayVariableInlined = arrayVariableInlined;
    }

    public DataFormat readDataFormat(URI uri) throws BinXException, IOException {
        return readDataFormat(uri, uri.toString());
    }

    public DataFormat readDataFormat(URI uri, String formatName) throws BinXException, IOException {
        DataFormat format = new DataFormat(parseDocument(uri));
        format.setName(formatName);
        for (Map.Entry<String, Type> entry : definitions.entrySet()) {
            format.addTypeDef(entry.getKey(), entry.getValue());
        }
        return format;
    }

    private CompoundType parseDocument(URI uri) throws IOException, BinXException {
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(uri.toURL());
        } catch (JDOMException e) {
            throw new BinXException(MessageFormat.format("Failed to read ''{0}''", uri), e);
        }
        Element binxElement = document.getRootElement();
        this.namespace = binxElement.getNamespace();
        parseParameters(binxElement);
        parseDefinitions(binxElement);
        return parseDataset(binxElement);
    }

    private void parseParameters(Element binxElement) throws BinXException {
        Element parametersElement = getChild(binxElement, "parameters", false);
        if (parametersElement != null) {
            // todo - implement parameters (nf - 2008-11-27)
            throw new BinXException(
                    MessageFormat.format("Element ''{0}'': Not implemented", parametersElement.getName()));
        }
    }

    private void parseDefinitions(Element binxElement) throws IOException, BinXException {
        Element definitionsElement = getChild(binxElement, "definitions", false);
        if (definitionsElement != null) {
            List defineTypeElements = getChildren(definitionsElement, "defineType", false);
            for (int i = 0; i < defineTypeElements.size(); i++) {
                Element defineTypeElement = (Element) defineTypeElements.get(i);
                String typeName = getTypeName(defineTypeElement, true);
                Element child = getChild(defineTypeElement, true);
                Type type = parseNonSimpleType(child);
                if (type == null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': ''{1}'' not expected here",
                                                                 defineTypeElement.getName(), child.getName()));
                }
                if (definitions.containsKey(typeName)) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Duplicate type definition ''{1}''",
                                                                 definitionsElement.getName(), typeName));
                }
                if (type instanceof CompoundType && type.getName().startsWith(ANONYMOUS_COMPOUND_PREFIX)) {
                    type = COMPOUND(typeName, ((CompoundType) type).getMembers());
                }
                definitions.put(typeName, type);
            }
        }
    }

    private CompoundType parseDataset(Element binxElement) throws BinXException, IOException {
        Element datasetElement = getChild(binxElement, "dataset", true);
        CompoundType compoundType = parseStruct(datasetElement);
        // inline single compound member
        if (singleDatasetStructInlined
            && compoundType.getMemberCount() == 1
            && compoundType.getMember(0).getType() instanceof CompoundType) {
            final CompoundMember member = compoundType.getMember(0);
            return COMPOUND(member.getName(), ((CompoundType) member.getType()).getMembers());
        } else {
            return COMPOUND("Dataset", compoundType.getMembers());
        }
    }

    private Type parseNonSimpleType(Element typeElement) throws BinXException {
        String childName = typeElement.getName();
        Type type = null;
        if (childName.equals("struct")) {
            type = parseStruct(typeElement);
        } else if (childName.equals("union")) {
            type = parseUnion(typeElement);
        } else if (childName.equals("arrayFixed")) {
            type = parseArrayFixed(typeElement);
        } else if (childName.equals("arrayStreamed")) {
            type = parseArrayStreamed(typeElement);
        } else if (childName.equals("arrayVariable")) {
            type = parseArrayVariable(typeElement);
        }
        return type;
    }

    private Type parseAnyType(Element typeElement) throws BinXException {
        String typeName = typeElement.getName();
        Type type;
        if (typeName.equals("useType")) {
            type = parseUseType(typeElement);
        } else {
            type = parseNonSimpleType(typeElement);
            if (type == null) {
                type = primitiveTypes.get(typeElement.getName());
                if (type == null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type: {1}",
                                                                 typeElement.getName(), typeName));
                }
            }
        }
        return type;
    }

    //    <useType typeName="Confidence_Descriptors_Data_Type" varName="Confidence_Descriptors_Data"/>
    //
    private Type parseUseType(Element typeElement) throws BinXException {
        String typeName = getTypeName(typeElement, true);
        Type type = definitions.get(typeName);
        if (type == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type definition: {1}",
                                                         typeElement.getName(), typeName));
        }
        return type;
    }

    //    <struct>
    //        <integer-32 varName="Days"/>
    //        <unsignedInteger-32 varName="Seconds"/>
    //        <unsignedInteger-32 varName="Microseconds"/>
    //    </struct>
    //
    private CompoundType parseStruct(Element typeElement) throws BinXException {
        final List memberElements = getChildren(typeElement, false);
        final ArrayList<CompoundMember> members = new ArrayList<CompoundMember>();

        for (int i = 0; i < memberElements.size(); i++) {
            final Element memberElement = (Element) memberElements.get(i);
            final Type memberType = parseAnyType(memberElement);

            if (memberType instanceof CompoundType) {
                final CompoundType compoundType = (CompoundType) memberType;

                // inline compound, if applicable
                if (inlinedStructs.contains(memberType.getName())) {
                    for (final CompoundMember compoundMember : compoundType.getMembers()) {
                        members.add(MEMBER(compoundMember.getName(), compoundMember.getType()));
                    }
                    continue;
                }
                // inline variable-length array, if applicable
                if (isArrayVariableInlined() && memberType.getName().startsWith(ARRAY_VARIABLE_PREFIX)) {
                    members.add(MEMBER(compoundType.getMemberName(0), compoundType.getMemberType(0)));
                    members.add(MEMBER(compoundType.getMemberName(1), compoundType.getMemberType(1)));
                    continue;
                }
            }

            final String memberName = getVarName(memberElement, true);
            members.add(MEMBER(memberName, memberType));
        }

        return COMPOUND(generateCompoundName(), members.toArray(new CompoundMember[members.size()]));
    }

    //    <arrayVariable varName="SM_SWATH" byteOrder="littleEndian">
    //        <sizeRef>
    //            <unsignedInteger-32 varName="N_Grid_Points"/>
    //        </sizeRef>
    //        <useType typeName="Grid_Point_Data_Type"/>
    //        <dim/>
    //    </arrayVariable>
    //
    private CompoundType parseArrayVariable(Element typeElement) throws BinXException {
        Element sizeRefElement = getChild(typeElement, "sizeRef", 0, true);
        Element arrayTypeElement = getChild(typeElement, 1, true);

        String sequenceName = getVarName(arrayTypeElement, false);
        if (sequenceName == null) {
            sequenceName = getVarName(typeElement, false);
            if (sequenceName == null) {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Missing name", typeElement.getName()));
            }
        }

        Element sizeRefTypeElement = getChild(sizeRefElement, true);
        String sizeRefName = getVarName(sizeRefTypeElement, false);
        if (sizeRefName == null) {
            sizeRefName = sequenceName + elementCountPostfix;
        }

        Type sizeRefType = parseAnyType(sizeRefTypeElement);
        if (!isIntegerType(sizeRefType)) {
            throw new BinXException(
                    MessageFormat.format("Element ''{0}'': 'sizeRef' must be an integer type", typeElement.getName()));
        }

        Type arrayType = parseAnyType(arrayTypeElement);
        SequenceType sequenceType = VAR_SEQUENCE(arrayType, sizeRefName);

        return COMPOUND(generateArrayVariableCompoundName(sequenceName),
                        MEMBER(sizeRefName, sizeRefType),
                        MEMBER(sequenceName, sequenceType));
    }

    private Type parseUnion(Element typeElement) throws BinXException {
        // todo - implement union  (nf - 2008-11-27)
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    //    <arrayFixed varName="Radiometric_Accuracy">
    //        <float-32/>
    //        <dim indexTo="1"/>
    //    </arrayFixed>
    private Type parseArrayFixed(Element typeElement) throws BinXException {
        Element arrayTypeElement = getChild(typeElement, 0, true);
        Element dimElement = getChild(typeElement, "dim", 1, true);

        if (!dimElement.getChildren().isEmpty()) {
            // todo - implement multi-dimensional arrays (rq - 2008-11-27)
            throw new BinXException(
                    MessageFormat.format("Element ''{0}'': Multi-dimensional arrays not yet implemented",
                                         typeElement.getName()));
        }

        final Type arrayType = parseAnyType(arrayTypeElement);
        final int indexFrom = getAttributeIntValue(dimElement, "indexFrom", 0);
        if (indexFrom != 0) {
            throw new BinXException(
                    MessageFormat.format("Element ''{0}'': Attribute 'indexFrom' other than zero not supported.",
                                         typeElement.getName()));
        }
        final int indexTo = getAttributeIntValue(dimElement, "indexTo");

        return SEQUENCE(arrayType, indexTo + 1);
    }

    private Type parseArrayStreamed(Element typeElement) throws BinXException {
        // todo - implement arrayStreamed  (nf - 2008-11-27)
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    private String getVarName(Element element, boolean require) throws BinXException {
        final String name = getAttributeValue(element, "varName", require);

        if (varNameMap.containsKey(name)) {
            return varNameMap.get(name);
        }

        return name;
    }

    private static String getTypeName(Element element, boolean require) throws BinXException {
        return getAttributeValue(element, "typeName", require);
    }

    private Element getChild(Element element, boolean require) throws BinXException {
        return getChild(element, 0, require);
    }

    private Element getChild(Element element, int index, boolean require) throws BinXException {
        return getChild(element, null, index, require);
    }

    private Element getChild(Element element, String name, boolean require) throws BinXException {
        final Element child = element.getChild(name, namespace);
        if (require && child == null) {
            throw new BinXException(MessageFormat.format(
                    "Element ''{0}}': child ''{1}'' not found.", element.getName(), name));
        }
        return child;
    }

    private Element getChild(Element element, String name, int index, boolean require) throws BinXException {
        final List children = getChildren(element, null, require);
        if (children.size() <= index) {
            if (require) {
                if (name != null) {
                    throw new BinXException(MessageFormat.format(
                            "Element ''{0}'': Expected to have a child ''{1}'' at index {2}", element.getName(), name,
                            index));
                } else {
                    throw new BinXException(MessageFormat.format(
                            "Element ''{0}'': Expected to have a child at index {1}", element.getName(), index));
                }
            } else {
                return null;
            }
        }
        final Element child = (Element) children.get(index);
        if (name != null && !name.equals(child.getName())) {
            throw new BinXException(MessageFormat.format(
                    "Element ''{0}'': Expected child ''{1}'' at index {2}", element.getName(), name, index));
        }
        return child;
    }

    private List getChildren(Element element, boolean require) throws BinXException {
        return getChildren(element, null, require);
    }

    private List getChildren(Element element, String name, boolean require) throws BinXException {
        final List children = element.getChildren(name, namespace);
        if (require && children.isEmpty()) {
            if (name != null) {
                throw new BinXException(MessageFormat.format(
                        "Element ''{0}'': Expected to have at least one child of ''{1}''", element.getName(), name));
            } else {
                throw new BinXException(MessageFormat.format(
                        "Element ''{0}'': Expected to have at least one child", element.getName()));
            }
        }
        return children;
    }

    private static String getAttributeValue(Element element, String name, boolean require) throws BinXException {
        final String value = element.getAttributeValue(name);
        if (require && value == null) {
            throw new BinXException(MessageFormat.format(
                    "Element ''{0}'': attribute ''{1}'' not found.", element.getName(), name));
        }
        return value != null ? value.trim() : value;
    }

    private static int getAttributeIntValue(Element element, String attributeName) throws BinXException {
        return getAttributeIntValue(element, attributeName, true);
    }

    private static int getAttributeIntValue(Element element, String attributeName, int defaultValue) throws BinXException {
        Integer value = getAttributeIntValue(element, attributeName, false);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private static Integer getAttributeIntValue(Element element, String attributeName, boolean required) throws BinXException {
        final String value = getAttributeValue(element, attributeName, required);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Attribute ''{1}'' must be an integer.",
                                                         element.getName(),
                                                         attributeName));
        }
    }

    private static String generateCompoundName() {
        return ANONYMOUS_COMPOUND_PREFIX + anonymousCompoundId++;
    }

    private static String generateArrayVariableCompoundName(String sequenceName) {
        return ARRAY_VARIABLE_PREFIX + sequenceName;
    }

    private static boolean isIntegerType(Type sizeRefType) {
        return sizeRefType == SimpleType.BYTE
               || sizeRefType == SimpleType.UBYTE
               || sizeRefType == SimpleType.SHORT
               || sizeRefType == SimpleType.USHORT
               || sizeRefType == SimpleType.INT
               || sizeRefType == SimpleType.UINT
               || sizeRefType == SimpleType.LONG
               || sizeRefType == SimpleType.ULONG;
    }
}
