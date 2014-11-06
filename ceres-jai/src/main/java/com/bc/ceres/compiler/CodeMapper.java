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

package com.bc.ceres.compiler;

import java.util.HashMap;
import java.util.Map;

public class CodeMapper {
    private static final char EOS = (char) -1;

    public static CodeMapping mapCode(CharSequence code, NameMapper nameMapper) {
        StringBuilder codeBuilder = new StringBuilder(2 * code.length());
        StringBuilder nameBuilder = new StringBuilder(16);
        Map<String, String> mappings = new HashMap<String, String>(16);
        int pos = 0;
        char ch;
        do {
            ch = pos < code.length() ? code.charAt(pos++) : EOS;
            if (nameBuilder.length() == 0) {
                if (Character.isJavaIdentifierStart(ch)) {
                    nameBuilder.append(ch);
                } else {
                    if (ch != EOS) {
                        codeBuilder.append(ch);
                    }
                }
            } else {
                if (Character.isJavaIdentifierPart(ch) || ch == '.') {
                    nameBuilder.append(ch);
                } else {
                    String name = nameBuilder.toString();
                    String mappedName = nameMapper.mapName(name);
                    if (mappedName != null) {
                        mappings.put(name, mappedName);
                        codeBuilder.append(mappedName);
                    } else {
                        codeBuilder.append(name);
                    }
                    if (ch != EOS) {
                        codeBuilder.append(ch);
                    }
                    nameBuilder.setLength(0);
                }
            }
        } while (ch != EOS);
        return new CodeMappingImpl(codeBuilder.toString(), mappings);
    }

    public static interface NameMapper {
        String mapName(String name);
    }

    public static interface CodeMapping {
        String getMappedCode();

        Map<String, String> getMappings();
    }

    private static class CodeMappingImpl implements CodeMapping {
        final String mappedCode;
        final Map<String, String> mappings;

        public CodeMappingImpl(String mappedCode, Map<String, String> mappings) {
            this.mappedCode = mappedCode;
            this.mappings = mappings;
        }

        public String getMappedCode() {
            return mappedCode;
        }

        public Map<String, String> getMappings() {
            return mappings;
        }
    }

}
