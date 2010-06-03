package com.bc.ceres.site.util;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic helper class which allows to generate and extend a file (plugins_list.csv). That file contains a number of
 * modules which are obtained by one or more given POMs, and are written in single row, comma separated.
 *
 * @author Thomas Storm
 * @version 1.0
 */
public class InclusionListBuilder {

    private static final String MODULE_NAME = "module";
    private static final String MODULES_NODE = "modules";
    public static final String INCLUSION_LIST_FILENAME = "plugins_list.csv";
    public static final char CSV_SEPARATOR = ',';

    static void parsePoms(File inclusionList, List<URL> poms) throws ParserConfigurationException, IOException,
                                                                            SAXException {
        for (URL pom : poms) {
            addPomToInclusionList(inclusionList, pom);
        }
    }

    static void addPomToInclusionList(File inclusionList, URL pom) throws ParserConfigurationException,
                                                                                 IOException, SAXException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(inclusionList, true));
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document w3cDoc = builder.parse(new File(pom.getFile()));
            final DOMBuilder domBuilder = new DOMBuilder();
            final org.jdom.Document doc = domBuilder.build(w3cDoc);
            final Element root = doc.getRootElement();
            final Namespace namespace = root.getNamespace();
            final List<Element> modules = root.getChildren(MODULES_NODE, namespace);
            if (modules != null) {
                // hard-coded index 0 is ok because xml-schema allows only one <modules>-node
                final Element modulesNode = modules.get(0);
                final List<Element> modulesList = (List<Element>) modulesNode.getChildren(MODULE_NAME, namespace);
                for (Element module : modulesList) {
                    addModuleToInclusionList(inclusionList, writer, module);
                    writer.write(CSV_SEPARATOR);
                }
            }
        } finally {
            writer.close();
        }

    }

    static void addModuleToInclusionList(File inclusionList, Writer writer, Element module) throws IOException {
        CsvReader reader = new CsvReader(new FileReader(inclusionList), new char[]{CSV_SEPARATOR});
        final String[] records = reader.readRecord();
        List<String> recordList = new ArrayList<String>();
        if (records != null) {
            recordList.addAll(Arrays.asList(records));
        }
        final String moduleName = module.getText();

        if (!recordList.contains(moduleName)) {
            writer.write(moduleName);
        }
    }

    public static File retrieveInclusionList(String repositoryUrl) {
        String sep = "/";
        if (repositoryUrl.endsWith(sep)) {
            sep = "";
        }
        try {
            return new File( new URI( repositoryUrl + sep + INCLUSION_LIST_FILENAME) );
        } catch (URISyntaxException e) {
            return null;
        }
    }
}