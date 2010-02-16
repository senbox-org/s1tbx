/*
 * HtmlGenerator.java
 *
 * Created on 19. April 2007, 10:05
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public interface HtmlGenerator {
    void generate(PrintWriter out, Module[] modules) throws IOException;
}
