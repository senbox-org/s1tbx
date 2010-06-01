/*
 * PageDecoratorGenerator.java
 *
 * Created on 19. April 2007, 10:07
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;

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
    
    public void generate(PrintWriter out, Module[] modules, String version) throws IOException {
        out.println("<html><head><title>Modules</title>" );
        out.println( generateStyleTag() );
        out.println("</head>");
        out.println("<body>");
        
        this.chain.generate(out, modules, version);
        
        out.println("</body></html>");
        out.flush();
    }

    private String generateStyleTag() throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append( "<style type=\"text/css\"> ");
        final URL modulesCss = getClass().getResource("modules.css");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader(modulesCss.toURI().getPath() ) );
        } catch (URISyntaxException e) {
        }
        String line;

        while( (line = reader.readLine() ) != null ) {
            builder.append( line );
        }
        builder.append( "</style>");
        reader.close();
        return builder.toString();
    }

}
