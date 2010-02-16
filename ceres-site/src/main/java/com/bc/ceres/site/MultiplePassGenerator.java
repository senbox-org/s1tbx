/*
 * MultiplePassGenerator.java
 *
 * Created on 25. April 2007, 14:25
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public class MultiplePassGenerator implements HtmlGenerator {

    private HtmlGenerator[] delegates;
    
    /** Creates a new instance of MultiplePassGenerator */
    public MultiplePassGenerator(HtmlGenerator[] delegates) {
        this.delegates = delegates;
    }

    public void generate(PrintWriter out, Module[] modules) throws IOException {
        for (HtmlGenerator generator : delegates) {
            generator.generate(out, modules);
        }
    }
    
}
