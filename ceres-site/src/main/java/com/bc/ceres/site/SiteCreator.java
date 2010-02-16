/*
 * SiteCreator.java
 *
 * Created on 19. April 2007, 09:54
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.internal.RepositoryScanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 *
 */
public class SiteCreator {
    private static Logger log = Logger.getLogger(SiteCreator.class.getName());
    
    /** Creates a new instance of SiteCreator */
    public SiteCreator() {
    }
    
    public static void main(String[] args) {
        if(args.length != 2) {
            printUsage(System.out);
            return;
        }
        URL moduleDir;
        try {
            moduleDir = new URL(args[0]);
        } catch (MalformedURLException ex) {
            System.err.println(ex.getClass().getName() + " " + ex.getMessage());
            printUsage(System.out);
            return;
        }
        System.out.println("Using Module Repository: " + moduleDir.toExternalForm());
        File outputDir = new File(args[1]);
        Assert.state(outputDir.exists(), "Output Directory " + outputDir.getAbsolutePath() + " not found");
        Assert.state(outputDir.isDirectory(), "Output Directory " + outputDir.getAbsolutePath() + " is not a directory");
        System.out.println("Using Output Directory: " + outputDir.getAbsolutePath());
        try {
            generate(moduleDir, outputDir);
            copyStaticContent(outputDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: java com.bc.ceres.SiteCreator moduleUrl outputDir");
        out.println("Where moduleUrl is the URL pointing to the repository and");
        out.println("outputDir is the directory where HTML should be generated to");
    }
    
    private static void copyStaticContent(File outputDir) throws IOException {
        copyResource("/modules.css", outputDir);
        copyResource("/module.png", outputDir);
    }
    
    private static void copyResource(String name, File outputDir) throws IOException {
        InputStream resource = SiteCreator.class.getResourceAsStream(name);
        FileOutputStream out = new FileOutputStream(new File(outputDir, name));
        int value = resource.read();
        while(value > -1) {
            out.write(value);
            value = resource.read();
        }
        resource.close();
        out.flush();
        out.close();
    }
    
    private static void generate(URL repositoryUrl, File outputDir) throws IOException, CoreException {
        RepositoryScanner moduleScanner = new RepositoryScanner(log, repositoryUrl, ProxyConfig.NULL);
        Module[] modules = moduleScanner.scan(ProgressMonitor.NULL);
        PrintWriter out = new PrintWriter(new FileOutputStream(new File(outputDir, "index.html")));
        
        HtmlGenerator generator = new PageDecoratorGenerator(
                new MultiplePassGenerator(
                    new HtmlGenerator[] {
                        new HtmlTocGenerator(), new HtmlModuleGenerator()
                    }
                ) );
        generator.generate(out, modules);
    }
}
