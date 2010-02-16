package com.bc.ceres.compiler;


import junit.framework.TestCase;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;

import com.bc.ceres.compiler.CodeCompiler;
import com.bc.ceres.compiler.CodeMapper;
import com.bc.ceres.jai.ExpressionCompilerConfig;


public final class CodeCompilerTest extends TestCase {

    public void testIt() throws Exception {
        String pattern = "" +
                "package com.bc.ceres.jai.opimage.exprc;\n" +
                "\n" +
                "import static java.lang.Math.*;\n" +
                "\n" +
                "public class {0} implements Function '{'\n" +
                "\n" +
                "{1}" +
                "\n" +
                "    public {0}(Product p) '{'\n" +
                "{2}" +
                "    }\n" +
                "\n" +
                "    public double eval(int _index) '{'\n" +
                "{3}" +
                "        return {4};\n" +
                "    }\n" +
                "}\n";

        Product p = new Product(new Band[]{
                new Band("b1", new double[]{1, 2, 3}),
                new Band("b2", new double[]{6, 7, 8}),
        });


        String className = "TestFunction";
        String expression = "0.5 * (b1 + b2)";
        CodeMapper.CodeMapping codeMapping = CodeMapper.mapCode(expression, new CodeMapper.NameMapper() {
            public String mapName(String name) {
                return name;
            }
        });

        StringBuilder varDecl = new StringBuilder();
        StringBuilder varDef = new StringBuilder();
        StringBuilder deref = new StringBuilder();
        for (Iterator<String> stringIterator = codeMapping.getMappings().keySet().iterator(); stringIterator.hasNext();) {
            String s = stringIterator.next();
            varDecl.append(String.format("    private final double[] a%s;\n", s));
            varDef.append(String.format("        a%s = p.getBand(\"%s\").getSamples();\n", s, s));
            deref.append(String.format("        final double %s = a%s[_index];\n", s, s));
        }

        final String code = MessageFormat.format(pattern,
                                                 className,
                                                 varDecl.toString(),
                                                 varDef.toString(),
                                                 deref.toString(),
                                                 expression);

        File outputDir = new File("./target/test-classes");
        File[] classPath = {new File("./target/classes"), new File("./target/test-classes")};
// todo - FIXME
//        Class<?> aClass = new CodeCompiler(new ExpressionCompilerConfig(outputDir, classPath)).compile("com.bc.ceres.jai.opimage.exprc", className, code);
//        final Function function = (Function) aClass.getConstructor(Product.class).newInstance(p);
//        assertEquals(0.5 * (1 + 6), function.eval(0), 1e-10);
//        assertEquals(0.5 * (2 + 7), function.eval(1), 1e-10);
//        assertEquals(0.5 * (3 + 8), function.eval(2), 1e-10);
    }

}