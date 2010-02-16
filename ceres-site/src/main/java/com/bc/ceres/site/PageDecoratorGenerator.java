/*
 * PageDecoratorGenerator.java
 *
 * Created on 19. April 2007, 10:07
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Decorate a Module-HTML in order to produce a complete HTML page. 
 * The page should be styled with an accompanying css
 */
public class PageDecoratorGenerator implements HtmlGenerator {
    
    private HtmlGenerator chain;
    
    /** Creates a new instance of PageDecoratorGenerator */
    public PageDecoratorGenerator(HtmlGenerator chain) {
        this.chain = chain;
    }
    
    public void generate(PrintWriter out, Module[] modules) throws IOException {
        out.println("<html><head><title>Ceres Modules</title>" );
        out.println("<link rel=\"stylesheet\" href=\"./modules.css\" type=\"text/css\" media=\"screen\" />");
        out.println("</head>");
        out.println("<body>");
        
        this.chain.generate(out, modules);
        
        out.println("</body></html>");
        out.flush();
    }
    
}
