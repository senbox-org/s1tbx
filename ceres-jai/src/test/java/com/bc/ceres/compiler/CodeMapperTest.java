package com.bc.ceres.compiler;

import junit.framework.TestCase;

import java.util.HashSet;

import com.bc.ceres.compiler.CodeMapper;

public class CodeMapperTest extends TestCase {
    public void testCodeMapper() {
        final HashSet<String> nameSet = new HashSet<String>();
        nameSet.add("b1");
        nameSet.add("b2");
        nameSet.add("b3");
        nameSet.add("b4");

        String expression = "b4 + sqrt(b1 * b1 + b2 * b2)";

        CodeMapper.NameMapper mapper = new CodeMapper.NameMapper() {
            public String mapName(String name) {
                return nameSet.contains(name) ? "_a" + name : null;
            }
        };
        CodeMapper.CodeMapping codeMapping = CodeMapper.mapCode(expression, mapper);

        assertNotNull(codeMapping);
        assertEquals("_ab4 + sqrt(_ab1 * _ab1 + _ab2 * _ab2)", codeMapping.getMappedCode());
        assertEquals(3, codeMapping.getMappings().size());
        assertEquals("_ab1", codeMapping.getMappings().get("b1"));
        assertEquals("_ab2", codeMapping.getMappings().get("b2"));
        assertEquals("_ab4", codeMapping.getMappings().get("b4"));
    }
}
