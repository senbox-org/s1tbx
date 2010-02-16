package com.bc.ceres.compiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * A file object used to represent Java source coming from a string.
 */
public class Code extends SimpleJavaFileObject {
    /**
     * The source code of this "file".
     */
    private final String className;

    /**
     * The source code of this "file".
     */
    private final String code;

    /**
     * Constructs a new JavaCode file object.
     *
     * @param className the fully qualified class name
     * @param code      the source code for the compilation unit represented by this file object
     */
    public Code(String className, String code) {
        super(toURI(className), Kind.SOURCE);
        this.className = className;
        this.code = code;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

    private static URI toURI(String className) {
        return URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension);
    }
}