package com.bc.ceres.binio.util;

import com.bc.ceres.binio.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple parser which can be used to read type definition <i>units</i> using the following syntax:
 * <blockquote>
 * <i>unit</i> := <i>compounds</i><br/>
 * <p/>
 * <i>compounds</i> := { <i>compound</i> }<br/>
 * <p/>
 * <i>compound</i> := <i>compound-name</i> <u>{</u> <i>members</i> <u>}</u>  [<u>;</u>]
 * <p/>
 * <i>members</i> := { <i>member</i> }<br/>
 * <p/>
 * <i>member</i> := <i>type</i> <i>member-name</i> <u>;</u> <br/>
 * <p/>
 * <i>type</i> := <i>scalar-type</i> | <i>array-type</i> <br/>
 * <p/>
 * <i>array-type</i> := <i>element-type</i> <u>[</u><i>element-count</i><u>]</u> { <u>[</u><i>element-count</i><u>]</u> } <br/>
 * <p/>
 * <i>element-type</i> := <i>scalar-type</i> <br/>
 * <p/>
 * <i>element-count</i> := <i>integer</i> | <i>member-reference</i> <br/>
 * <p/>
 * <i>scalar-type</i> := <i>simple-type</i> | <i>compound-name</i> <br/>
 * <p/>
 * <i>simple-type</i> := <u>byte</u> | <u>ubyte</u> | <u>short</u> | <u>ushort</u> | <u>int</u> | <u>uint</u> | <u>long</u> | <u>float</u> | <u>double</u> <br/>
 * <p/>
 * <i>member-reference</i> := <u>$</u><i>member-name</i> (member must be an integer type) <br/>
 * <p/>
 * <i>compound-name</i> := <i>name</i> <br/>
 * <p/>
 * <i>member-name</i> := <i>name</i> <br/>
 * <p/>
 * <i>name</i> := <i>java-identifier</i> | any character sequence within two enclosing <u>"</u> (double quote)<br/>
 * </blockquote>
 * <p/>
 * <p/>
 * For example:
 * <pre>
 * <p/>
 * Dataset {
 *     int lineCount;
 *     Scanline[$lineCount] scanlines;
 * };
 * <p/>
 * Scanline {
 *     int flags;
 *     double[512] data;
 * };
 * </pre>
 */
public class TypeParser {
    private final HashMap<String, SimpleType> simpleTypeMap;
    private final HashMap<String, CompoundType> compoundTypeMap;
    private final HashMap<String, Typedef> typedefMap;
    private final StreamTokenizer st;

    private TypeParser(StreamTokenizer st) {
        this.st = st;
        this.compoundTypeMap = new HashMap<String, CompoundType>(11);
        this.typedefMap = new HashMap<String, Typedef>(11);
        this.simpleTypeMap = new HashMap<String, SimpleType>(11);
        for (SimpleType type : SimpleType.TYPES) {
            registerSimpleType(type);
        }
    }

    private void registerSimpleType(SimpleType type) {
        simpleTypeMap.put(type.getName(), type);
    }

    public static CompoundType[] parseUnit(Reader reader) throws IOException, ParseException {
        StreamTokenizer st = new StreamTokenizer(reader);
        st.resetSyntax();
        st.eolIsSignificant(false);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.parseNumbers();
        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars('_', '_');
        st.wordChars('0', '9');
        st.whitespaceChars(0, ' ');
        st.quoteChar('"');
        TypeParser typeParser = new TypeParser(st);
        CompoundType[] compoundTypes = typeParser.parseCompoundTypes();
        typeParser.resolve(compoundTypes);
        return compoundTypes;
    }

    private void resolve(CompoundType[] compoundTypes) throws ParseException {
        for (CompoundType compoundType : compoundTypes) {
            resolve(compoundType);
        }
    }

    private CompoundType resolve(CompoundType compoundType) throws ParseException {
        for (int i = 0; i < compoundType.getMemberCount(); i++) {
            CompoundType.Member member = compoundType.getMember(i);
            Type memberType = member.getType();
            Type resolvedMemberType = resolveType(memberType);
            compoundType.setMember(i, new CompoundType.Member(member.getName(), resolvedMemberType));
        }
        return compoundType;
    }

    private Type resolveType(Type type) throws ParseException {
        Type resolvedType;
        if (type instanceof Typedef) {
            resolvedType = resolve((Typedef) type);
        } else if (type instanceof CompoundType) {
            resolvedType = resolve((CompoundType) type);
        } else if (type instanceof SequenceType) {
            resolvedType = resolve((SequenceType) type);
        } else {
            resolvedType = type;
        }
        return resolvedType;
    }

    private Type resolve(Typedef typedef) throws ParseException {
        Type resolvedType;
        if (typedef.getType() != null) {
            resolvedType = typedef.getType();
        } else {
            CompoundType compoundType = compoundTypeMap.get(typedef.getName());
            if (compoundType == null) {
                error(st, "Unresolved type: " + typedef.getName() + ".");
            }
            typedef.setType(compoundType);
            resolvedType = compoundType;
        }
        return resolvedType;
    }

    private Type resolve(SequenceType sequenceType) throws ParseException {
        return new SequenceType(resolveType(sequenceType.getElementType()), sequenceType.getElementCount());
    }

    public CompoundType[] parseCompoundTypes() throws IOException, ParseException {
        ArrayList<CompoundType> list = new ArrayList<CompoundType>(32);
        while (true) {
            CompoundType compoundType = parseCompoundType();
            if (compoundType == null) {
                break;
            }
            list.add(compoundType);
            compoundTypeMap.put(compoundType.getName(), compoundType);
        }
        return list.toArray(new CompoundType[list.size()]);
    }

    private CompoundType parseCompoundType() throws IOException, ParseException {

        final String name = parseName();
        if (name == null) {
            return null;
        }
        int token;
        token = st.nextToken();
        if (token != '{') {
            error(st, "'{' expected.");
        }
        CompoundType.Member[] members = parseMembers();
        token = st.nextToken();
        if (token != '}') {
            st.pushBack();
            error(st, "'}' expected.");
        }
        token = st.nextToken();
        if (token != ';') {
            st.pushBack();
        }
        return new CompoundType(name, members);
    }

    private String parseName() throws IOException {
        int token = st.nextToken();
        final String name;
        if (token == StreamTokenizer.TT_WORD) {
            name = st.sval;
        } else if (token == '"') {
            name = st.sval;
        } else {
            st.pushBack();
            name = null;
        }
        return name;
    }

    private CompoundType.Member[] parseMembers() throws IOException, ParseException {
        ArrayList<CompoundType.Member> list = new ArrayList<CompoundType.Member>(32);
        while (true) {
            final CompoundType.Member member = parseMember();
            if (member == null) {
                break;
            }
            list.add(member);
        }
        return list.toArray(new CompoundType.Member[list.size()]);
    }

    private CompoundType.Member parseMember() throws IOException, ParseException {
        Type type = parseType();
        if (type == null) {
            return null;
        }
        String name = parseName();
        if (name == null) {
            error(st, "Member name expected.");
        }
        int token = st.nextToken();
        if (token != ';') {
            st.pushBack();
            error(st, "';' expected.");
        }
        return new CompoundType.Member(name, type);
    }

    private Type parseType() throws IOException, ParseException {
        String name = parseName();
        if (name == null) {
            return null;
        }
        Type type = simpleTypeMap.get(name);
        if (type == null) {
            type = compoundTypeMap.get(name);
            if (type == null) {
                type = typedefMap.get(name);
                if (type == null) {
                    Typedef typedef = new Typedef(name);
                    typedefMap.put(name, typedef);
                    type = typedef;
                }
            }
        }
        while (true) {
            int token = st.nextToken();
            if (token == '[') {
                token = st.nextToken();
                if (token == StreamTokenizer.TT_NUMBER) {
                    int elementCount = (int) st.nval;
                    if (elementCount != st.nval) {
                        error(st, "Integer element count expected.");
                    }
                    token = st.nextToken();
                    if (token != ']') {
                        error(st, "']' expected.");
                    }
                    type = new SequenceType(type, elementCount);
                } else if (token == ']') {
                    type = new SequenceType(type);
                } else {
                    st.pushBack();
                    error(st, "Element count expected after '['.");
                }
            } else {
                st.pushBack();
                break;
            }
        }
        return type;
    }

    private static void error(StreamTokenizer st, String s) throws ParseException {
        throw new ParseException(s, st.lineno());
    }

    private static class Typedef extends Type {
        private final String name;
        private Type type;

        private Typedef(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return type != null ? type.getSize() : -1;
        }

        @Override
        public boolean isSimpleType() {
            return type != null && type.isSimpleType();
        }

        @Override
        public boolean isSequenceType() {
            return type != null && type.isSequenceType();
        }

        @Override
        public boolean isCompoundType() {
            return type != null && type.isCompoundType();
        }

        @Override
        public void visit(TypeVisitor visitor) {
            if (type != null) {
                type.visit(visitor);
            }
        }
    }
}
