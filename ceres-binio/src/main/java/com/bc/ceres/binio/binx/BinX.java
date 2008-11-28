package com.bc.ceres.binio.binx;

import com.bc.ceres.binio.*;
import static com.bc.ceres.binio.TypeBuilder.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

/**
 * See the <a href="http://www.edikt.org/binx/">BinX Project</a>.
 */
public class BinX {
    static final String ANONYMOUS_COMPOUND_PREFIX = "AnonymousCompound@";
    static final String ARRAY_VARIABLE_PREFIX = "ArrayVariable@";
    static final String DEFAULT_ELEMENT_COUNT_POSTFIX = "_Counter";

    private  URI uri;

    private final Map<String, String> parameters;
    private final Map<String, Type> definitions;

    private String elementCountPostfix;

    private Map<String, SimpleType> primitiveTypes;

    private Namespace namespace;

    private static int anonymousCompoundId = 0;

    public BinX() {

        parameters = new HashMap<String, String>();
        definitions = new HashMap<String, Type>();

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
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String setParameter(String name, String value) {
        return parameters.put(name, value);
    }

    public Type getDefinition(String name) {
        return definitions.get(name);
    }

    public Type setDefinition(String name, Type value) {
        return definitions.put(name, value);
    }

    Map<String, SimpleType> getPrimitiveTypes() {
        return primitiveTypes;
    }

    private CompoundType parseDocument(URI uri) throws IOException, BinXException {
        this.uri = uri;
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(uri.toURL());
        } catch (JDOMException e) {
            throw new BinXException(MessageFormat.format("Failed to read ''{0}''", uri), e);
        }
        Element binxElement = document.getRootElement();
        namespace = binxElement.getNamespace();
        parseParameters(binxElement);
        parseDefinitions(binxElement);
        return parseDataset(binxElement);
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

    private void parseParameters(Element binxElement) throws BinXException {
        Element parametersElement = getChild(binxElement, "parameters", false);
        if (parametersElement != null) {
            // todo - implement parameters (nf - 2008-11-27)
            throw new BinXException(MessageFormat.format("Element ''{0}'': Not implemented", parametersElement.getName()));
        }
    }

    private void parseDefinitions(Element binxElement) throws IOException, BinXException {
        Element definitionsElement = getChild(binxElement, "definitions", false);
        if (definitionsElement != null) {
            List defineTypeElements = getChildren(definitionsElement, "defineType", false);
            for (int i = 0; i < defineTypeElements.size(); i++) {
                Element defineTypeElement = (Element) defineTypeElements.get(i);
                String typeName = getAttributeValue(defineTypeElement, "typeName", true);
                Element child = getChild(defineTypeElement, true);
                Type type = parseNonSimpleType(child);
                if (type == null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': ''{1}'' not expected here", defineTypeElement.getName(), child.getName()));
                }
                if (definitions.containsKey(typeName)) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Duplicate type definition ''{1}''", definitionsElement.getName(), typeName));
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
        // todo - introduce property for this behaviour (rq - 28.11.2008)
        if (compoundType.getMemberCount() == 1 && compoundType.getMember(0).getType() instanceof CompoundType) {
            final CompoundMember member = compoundType.getMember(0);
            return COMPOUND(member.getName(), ((CompoundType)member.getType()).getMembers());
        } else {
            return  COMPOUND("Dataset", compoundType.getMembers());
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
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type: {1}", typeElement.getName(), typeName));
                }
            }
        }
        return type;
    }

    //    <useType typeName="Confidence_Descriptors_Data_Type" varName="Confidence_Descriptors_Data"/>
    //
    private Type parseUseType(Element typeElement) throws BinXException {
        String typeName = getAttributeValue(typeElement, "typeName", true);
        Type type = definitions.get(typeName);
        if (type == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type definition: {1}", typeElement.getName(), typeName));
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
        List memberElements = getChildren(typeElement, false);
        ArrayList<CompoundMember> members = new ArrayList<CompoundMember>();

        for (int i = 0; i < memberElements.size(); i++) {
            Element memberElement = (Element) memberElements.get(i);
            String memberName = getAttributeValue(memberElement, "varName", true);
            Type memberType = parseAnyType(memberElement);
            // inline variable-length arrays
            // todo - introduce property for this behaviour (rq - 28.11.2008)
            if (memberType instanceof CompoundType && memberType.getName().startsWith(ARRAY_VARIABLE_PREFIX)) {
                CompoundType compoundType = (CompoundType) memberType;
                members.add(MEMBER(compoundType.getMemberName(0), compoundType.getMemberType(0)));
                members.add(MEMBER(compoundType.getMemberName(1), compoundType.getMemberType(1)));
            } else {
                members.add(MEMBER(memberName, memberType));
            }
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
        Element dimElement = getChild(typeElement, "dim", 2, true);

        String sequenceName = getAttributeValue(typeElement, "varName", false);
        if (sequenceName == null) {
            sequenceName = getAttributeValue(dimElement, "name", false);
            if (sequenceName == null) {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Missing name", typeElement.getName()));
            }
        }

        Element sizeRefTypeElement = getChild(sizeRefElement, true);
        String sizeRefName = getAttributeValue(sizeRefTypeElement, "varName", false);
        if (sizeRefName == null) {
            sizeRefName = sequenceName + DEFAULT_ELEMENT_COUNT_POSTFIX;
        }

        Type sizeRefType = parseAnyType(sizeRefTypeElement);
        if (!isIntegerType(sizeRefType)) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': 'sizeRef' must be an integer type", typeElement.getName()));
        }

        Type arrayType = parseAnyType(arrayTypeElement);
        SequenceType sequenceType = VAR_SEQUENCE(arrayType, sizeRefName);

        return COMPOUND(generateArrayVariableCompoundName(sequenceName),
                        MEMBER(sizeRefName, sizeRefType),
                        MEMBER(sequenceName, sequenceType));
    }

    private boolean isIntegerType(Type sizeRefType) {
        return sizeRefType == SimpleType.BYTE
                || sizeRefType == SimpleType.UBYTE
                || sizeRefType == SimpleType.SHORT
                || sizeRefType == SimpleType.USHORT
                || sizeRefType == SimpleType.INT
                || sizeRefType == SimpleType.UINT
                || sizeRefType == SimpleType.LONG
                || sizeRefType == SimpleType.ULONG;
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

        return SEQUENCE(arrayType, indexTo);
    }

    private Type parseArrayStreamed(Element typeElement) throws BinXException {
        // todo - implement arrayStreamed  (nf - 2008-11-27)
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    private String getAttributeValue(Element element, String name, boolean require) throws BinXException {
        String value = element.getAttributeValue(name);
        if (require && value == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': attribute ''{1}'' not found.", element.getName(), name));
        }
        return value;
    }

    private int getAttributeIntValue(Element element, String attributeName) throws BinXException {
        return getAttributeIntValue(element, attributeName, true);
    }

    private int getAttributeIntValue(Element element, String attributeName, int defaultValue) throws BinXException {
        Integer value = getAttributeIntValue(element, attributeName, false);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private Integer getAttributeIntValue(Element element, String attributeName, boolean required) throws BinXException {
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

    private Element getChild(Element element, boolean require) throws BinXException {
        return getChild(element, 0, require);
    }

    private Element getChild(Element element, int index, boolean require) throws BinXException {
        return getChild(element, null, index, require);
    }

    private Element getChild(Element element, String name, boolean require) throws BinXException {
        Element child = element.getChild(name, namespace);
        if (require && child == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}}': child ''{1}'' not found.", element.getName(), name));
        }
        return child;
    }

    private Element getChild(Element element, String name, int index, boolean require) throws BinXException {
        List children = getChildren(element, null, require);
        if (children.size() <= index) {
            if (require) {
                if (name != null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have a child ''{1}'' at index {2}", element.getName(), name, index));
                } else {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have a child at index {1}", element.getName(), index));
                }
            } else {
                return null;
            }
        }
        Element child = (Element) children.get(index);
        if (name != null && !name.equals(child.getName())) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Expected child ''{1}'' at index {2}", element.getName(), name, index));
        }
        return child;
    }

    private List getChildren(Element element, boolean require) throws BinXException {
        return getChildren(element, null, require);
    }

    private List getChildren(Element element, String name, boolean require) throws BinXException {
        List children = element.getChildren(name, namespace);
        if (require && children.isEmpty()) {
            if (name != null) {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have at least one child of ''{1}''", element.getName(), name));
            } else {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have at least one child", element.getName()));
            }
        }
        return children;
    }

    private String generateCompoundName() {
        return ANONYMOUS_COMPOUND_PREFIX + anonymousCompoundId++;
    }

    private String generateArrayVariableCompoundName(String sequenceName) {
        return ARRAY_VARIABLE_PREFIX + sequenceName;
    }
}
