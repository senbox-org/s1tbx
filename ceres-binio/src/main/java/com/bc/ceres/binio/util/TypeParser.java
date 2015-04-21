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

package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.internal.CompoundTypeImpl;
import com.bc.ceres.binio.internal.VarElementCountSequenceTypeImpl;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * A simple parser which can be used to read type definition <i>units</i> using the following syntax:
 * <blockquote>
 * <i>unit</i> := <i>compounds</i>
 * <p>
 * <i>compounds</i> := { <i>compound</i> }
 * <p>
 * <i>compound</i> := <i>compound-name</i> <u>{</u> <i>members</i> <u>}</u>  [<u>;</u>]
 * <p>
 * <i>members</i> := { <i>member</i> }
 * <p>
 * <i>member</i> := <i>type</i> <i>member-name</i> <u>;</u>
 * <p>
 * <i>type</i> := <i>scalar-type</i> | <i>array-type</i>
 * <p>
 * <i>array-type</i> := <i>element-type</i> <u>[</u><i>element-count</i><u>]</u> { <u>[</u><i>element-count</i><u>]</u> }
 * <p>
 * <i>element-type</i> := <i>scalar-type</i>
 * <p>
 * <i>element-count</i> := <i>integer</i> | <i>member-reference</i>
 * <p>
 * <i>scalar-type</i> := <i>simple-type</i> | <i>compound-name</i>
 * <p>
 * <i>simple-type</i> := <u>byte</u> | <u>ubyte</u> | <u>short</u> | <u>ushort</u> | <u>int</u> | <u>uint</u> | <u>long</u> | <u>float</u> | <u>double</u>
 * <p>
 * <i>member-reference</i> := <u>$</u><i>member-name</i> (member must be an integer type)
 * <p>
 * <i>compound-name</i> := <i>name</i>
 * <p>
 * <i>member-name</i> := <i>name</i>
 * <p>
 * <i>name</i> := <i>java-identifier</i> | any character sequence within two enclosing <u>"</u> (double quote)
 * </blockquote>
 * For example:
 * <pre>
 * Dataset {
 *     int lineCount;
 *     Scanline[$lineCount] scanlines;
 * };
 * Scanline {
 *     int flags;
 *     double[512] data;
 * };
 * </pre>
 */
public class TypeParser {
    private final HashMap<String, SimpleType> simpleTypeMap;
    private final HashMap<String, CompoundType> compoundTypeMap;
    private final StreamTokenizer st;
    private static final String UNRESOLVED = "Unresolved@";
    public final static SimpleType[] SIMPLE_TYPES = {
            SimpleType.BYTE, SimpleType.UBYTE,
            SimpleType.SHORT, SimpleType.USHORT,
            SimpleType.INT, SimpleType.UINT,
            SimpleType.LONG, SimpleType.ULONG,
            SimpleType.FLOAT, SimpleType.DOUBLE
    };

    private TypeParser(StreamTokenizer st) {
        this.st = st;
        this.compoundTypeMap = new HashMap<String, CompoundType>(11);
        this.simpleTypeMap = new HashMap<String, SimpleType>(11);
        for (SimpleType type : SIMPLE_TYPES) {
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

    private Type resolveType(Type type) throws ParseException {
        Type resolvedType;
        if (type instanceof CompoundType) {
            resolvedType = resolve((CompoundType) type);
        } else if (type instanceof SequenceType) {
            resolvedType = resolve((SequenceType) type);
        } else {
            resolvedType = type;
        }
        return resolvedType;
    }

    private CompoundType resolve(CompoundType compoundType) throws ParseException {

        if (compoundType.getName().startsWith(UNRESOLVED)) {
            String name = compoundType.getName().substring(UNRESOLVED.length());
            CompoundType resolvedType = compoundTypeMap.get(name);
            if (resolvedType == null) {
                throw new ParseException("Unresolved compound type: " + name, -1);
            }
            return resolvedType;
        }

        for (int i = 0; i < compoundType.getMemberCount(); i++) {
            CompoundMember member = compoundType.getMember(i);
            Type memberType = member.getType();
            Type resolvedMemberType = resolveType(memberType);
            ((CompoundTypeImpl) compoundType).setMember(i, MEMBER(member.getName(), resolvedMemberType));
        }

        return compoundType;
    }

    private Type resolve(SequenceType sequenceType) throws ParseException {
        if (sequenceType instanceof VarElementCountSequenceTypeImpl) {
            VarElementCountSequenceTypeImpl varSequenceType = (VarElementCountSequenceTypeImpl) sequenceType;
            return VAR_SEQUENCE(resolveType(sequenceType.getElementType()), varSequenceType.getMemberName());
        } else {
            return SEQUENCE(resolveType(sequenceType.getElementType()), sequenceType.getElementCount());
        }
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
        CompoundMember[] members = parseMembers(name);
        token = st.nextToken();
        if (token != '}') {
            st.pushBack();
            error(st, "'}' expected.");
        }
        token = st.nextToken();
        if (token != ';') {
            st.pushBack();
        }
        return COMPOUND(name, members);
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

    private CompoundMember[] parseMembers(String parentCompoundName) throws IOException, ParseException {
        ArrayList<CompoundMember> list = new ArrayList<CompoundMember>(32);
        while (true) {
            final CompoundMember member = parseMember(parentCompoundName);
            if (member == null) {
                break;
            }
            list.add(member);
        }
        return list.toArray(new CompoundMember[list.size()]);
    }

    private CompoundMember parseMember(String parentCompoundName) throws IOException, ParseException {
        Type type = parseType(parentCompoundName);
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
        return MEMBER(name, type);
    }

    private Type parseType(String parentCompoundName) throws IOException, ParseException {
        String name = parseName();
        if (name == null) {
            return null;
        }
        Type type = simpleTypeMap.get(name);
        if (type == null) {
            type = compoundTypeMap.get(name);
            if (type == null) {
                CompoundType unresolvedType = COMPOUND(UNRESOLVED + name);
                compoundTypeMap.put(name, unresolvedType);
                type = unresolvedType;
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
                    type = SEQUENCE(type, elementCount);
                } else if (token == StreamTokenizer.TT_WORD) {
                    String lengthRefName = st.sval;
                    if (lengthRefName.indexOf('.') == -1) {
                        lengthRefName = parentCompoundName + "." + lengthRefName;
                    }
                    type = VAR_SEQUENCE(type, lengthRefName);
                    token = st.nextToken();
                    if (token != ']') {
                        error(st, "']' expected.");
                    }
                } else if (token == ']') {
                    type = SEQUENCE(type, -1);
                } else {
                    st.pushBack();
                    error(st, "Array length specifier expected after '['.");
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
}
