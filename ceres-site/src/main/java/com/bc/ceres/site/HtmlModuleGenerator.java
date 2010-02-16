/*
 * HtmlModuleGenerator.java
 *
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
 * Generate a HTML view of a module repository. This is only a fragment, not a
 * complete page.
 * @see PageDecoratorGenerator for information on how to create a complete HTML page
 */
public class HtmlModuleGenerator implements HtmlGenerator {

    private final ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/bc/ceres/site/LocalStrings");

    /** Creates a new instance of HtmlModuleGenerator */
    public HtmlModuleGenerator() {
    }

    public void generate(PrintWriter out, Module[] modules) throws IOException {
        out.println("<table class=\"modules\">");
        for (Module module : modules) {
            out.println("  <tr class=\"head\">");
            out.print("    <td class=\"name\"><a name=\"" + sanitize(module.getName().replace(' ', '_'))+"\"></a>");
            out.print(getDisplayText("name"));
            out.println("</td><td class=\"value\"><a href=\"" +  sanitize(module.getLocation().toExternalForm()) + "\">"+sanitize(module.getName())+"</a></td></tr>");
//            output(out, "name", module.getName(), module.getLocation().toExternalForm());
            output(out, "symbolicName", module.getSymbolicName(), null);
            output(out, "aboutUrl", module.getAboutUrl()!=null?"<span class=\"aboutLink\">about</span>":"", module.getAboutUrl());
            output(out, "activatorClassName", module.getActivatorClassName(), null);
            output(out, "vendor", module.getVendor(), null);
            output(out, "contactAddress", module.getContactAddress(), null);
            output(out, "url", module.getUrl(), module.getUrl());
            output(out, "copyright", module.getCopyright(), null);
            output(out, "description", module.getDescription(), null);
            output(out, "licenseUrl", module.getLicenseUrl(), module.getLicenseUrl());
            output(out, "manifestVersion", module.getManifestVersion(), null);
            output(out, "packaging", module.getPackaging(), null);
            output(out, "version", module.getVersion().toString(), null);
            output(out, "contentLength", getSizeText(module.getContentLength()), null);
            output(out, "lastModified", getDateText(module.getLastModified()), null);

            out.println("  <tr class=\"totop\"><td></td><td><a href=\"#top\">top</a></td></tr>");
        }
        out.println("</table>");
    }

    private void output(PrintWriter out, String clazz) {
        output(out, clazz, "", null);
    }

    private void output(PrintWriter out, String clazz, String value, String link) {
        clazz = sanitize(clazz);
        value = sanitize(value);
        link = sanitize(link);
        out.print("<tr class=\""+clazz+"\"><td class=\"key\">");
        out.print(getDisplayText(clazz));
        out.print("</td><td class=\"value\">");
        if(link != null) {
            out.print("<a href=\"" + link + "\">") ;
        }
        if(value != null) {
            out.print(value.trim());
        }
        if(link != null) out.print("</a>") ;
        out.println("</td>\n</tr>");
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
        return s == null ? NOT_SPECIFIED : s;
    }

    static String getDateText(long timestamp) {
        DateFormat dateInstance = SimpleDateFormat.getDateInstance();
        return dateInstance.format(new Date(timestamp));
    }

    static String getSizeText(long bytes) {
        long kilos = Math.round(bytes / 1024.0);
        long megas = Math.round(bytes / (1024.0 * 1024.0));
        return getText(megas > 0 ? (megas + " M") : kilos > 0 ? (kilos + " K") : (bytes + " B"));
    }

    private String sanitize(String link) {
        // todo: escape HTML special characters.
        return link;
    }
}
