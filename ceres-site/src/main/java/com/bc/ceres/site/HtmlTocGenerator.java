/*
 * HtmlTocGenerator.java
 * Generates a table of content
 * Created on 19. April 2007, 10:08
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Generate a HTML view of a module repositories Table Of Content.
 * This is only a fragment, not a complete page.
 * @see PageDecoratorGenerator for information on how to create a complete HTML page
 */
public class HtmlTocGenerator implements HtmlGenerator {

    private final ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/bc/ceres/site/LocalStrings");

    /** Creates a new instance of HtmlModuleGenerator */
    public HtmlTocGenerator() {
    }

    public void generate(PrintWriter out, Module[] modules) throws IOException {
        int line = 0;
        out.println("<a name=\"top\"></a>");
        out.println("<h1>BEAM 4.0 modules</h1>");
        out.println("<table class=\"toc_modules\">");
        out.println("  <tr class=\"head\">");
        output(out, "name");
        output(out, "symbolicName");
        output(out, "vendor");
        output(out, "version");
        output(out, "lastModified");
        out.println("  </tr>");
//        out.println("  <div class=\"body\">");

        for (Module module : modules) {
            if(line++ % 2 == 0){
                out.println("  <tr class=\"even\">");
            } else {
                out.println("  <tr class=\"odd\">");
            }
            output(out, "name", module.getName(), "#" + module.getName().replace(' ', '_'));
            output(out, "symbolicName", module.getSymbolicName(), null);
            output(out, "vendor", module.getVendor(), null);
            output(out, "version", module.getVersion().toString(), null);
            output(out, "lastModified", getDateText(module.getLastModified()), null);

            out.println("  </tr>");
        }
 //       out.println("  </div>");
        out.println("</table>\n<br/><br/>");
    }

    private void output(PrintWriter out, String clazz) {
        out.print("    <td class=\"");
        out.print(clazz);
        out.print("\">");
        out.print(getDisplayText(clazz));
        out.println("</td>");
    }

    private void output(PrintWriter out, String clazz, String value, String link) {
        out.print("      <td class=\"");
        out.print(clazz);
        out.print("\">");
        if(link != null) {
            out.print("<a href=\"" + link + "\">") ;
        }
        if(value != null) {
            out.print(value.trim());
        }
        if(link != null) out.print("</a>") ;
        out.println("</td>");
    }

    private String getDisplayText(String key) {
        String bundleKey = "module." + key;
        if(bundle.containsKey(bundleKey)) {
            return bundle.getString(bundleKey).trim();
        }
        return key.trim();
    }

    // siehe ModuleTextFactory in ceres-ui... Dieser Code wurde in der Eile
    // kopiert und modifiziert!

    private static final String NOT_SPECIFIED = "(not specified)";

    static String getText(String s) {
        return s == null || s.length() == 0 ? NOT_SPECIFIED : s;
    }

    static String getDateText(long timestamp) {
        DateFormat dateInstance = SimpleDateFormat.getDateInstance();
        return dateInstance.format(new Date(timestamp));
    }

}
