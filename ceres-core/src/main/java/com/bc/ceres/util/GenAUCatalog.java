package com.bc.ceres.util;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * This tool generates the catalog (update.xml/update.xml.gz) for a set of nbm files.
 * The tool takes up to three parameters:
 * <ol>
 * <li>The path to the directory containing the nbm files (mandatory)</li>
 * <li>A notification message which is shown to the user as a balloon message in the lower right (optional)</li>
 * <li>An URL for the notification message (optional)</li>
 * </ol>
 */
public class GenAUCatalog {

    private static final String NBM_FILE_EXTENSION = ".nbm";
    private static final String PATH_INFO_XML = "/Info/info.xml";
    private static final String FILE_NAME_CATALOG_XML = "updates.xml";
    private static final String FILE_NAME_CATALOG_XML_GZ = FILE_NAME_CATALOG_XML + ".gz";
    private static final String TAG_NAME_LICENSE = "license";
    private static final String TAG_NAME_MODULE = "module";
    private static final String ATTRIB_NAME_DOWNLOADSIZE = "downloadsize";
    private static final String ATTRIB_NAME_NAME = "name";
    private static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" +
                                           "<!DOCTYPE module_updates PUBLIC \"-//NetBeans//DTD Autoupdate Catalog 2.5//EN\" \"http://www.netbeans.org/dtds/autoupdate-catalog-2_5.dtd\">\r\n\r\n";

    private final Path moduleDir;
    private final SimpleDateFormat timeStampFormat;
    private Transformer xmlTransformer;
    private DocumentBuilderFactory builderFactory;
    private Path catalogXmlPath;
    private Path catalogXmlGzPath;
    private String notificationMessage;
    private String notificationURL;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            logErr("Please specify at least a path to a directory which contains NetBeans modules (*.nbm)");
        }
        Path moduleDir = Paths.get(args[0]);
        GenAUCatalog generator = new GenAUCatalog(moduleDir);
        if (args.length > 1) {
            generator.setNotificationMessage(args[1]);
        }
        if (args.length > 2) {
            generator.setNotificationURL(args[2]);
        }
        generator.run();
    }

    public GenAUCatalog(Path moduleDir) throws Exception {
        this.moduleDir = moduleDir;
        catalogXmlPath = moduleDir.resolve(FILE_NAME_CATALOG_XML);
        catalogXmlGzPath = moduleDir.resolve(FILE_NAME_CATALOG_XML_GZ);
        if (Files.exists(catalogXmlPath)) {
            throw new Exception(String.format("File %s already exists", catalogXmlPath));
        }
        if (Files.exists(catalogXmlGzPath)) {
            throw new Exception(String.format("File %s already exists", catalogXmlGzPath));
        }

        timeStampFormat = new SimpleDateFormat("ss/mm/HH/dd/MM/yyyy");
        timeStampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        builderFactory.setExpandEntityReferences(false);
        xmlTransformer = TransformerFactory.newInstance().newTransformer();
        xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
    }

    public void setNotificationMessage(String msg) {
        this.notificationMessage = msg;
    }

    public void setNotificationURL(String url) {
        this.notificationURL = url;
    }

    private void run() throws Exception {

        ArrayList<Node> moduleList = new ArrayList<>();
        HashMap<String, Node> licenseMap = new HashMap<>();

        final Stream<Path> nbmFiles = Files.list(moduleDir).filter(this::isNbmModuleFile);
        nbmFiles.forEach(path -> processModule(path, moduleList, licenseMap));

        log(String.format("Found %d modules in directory", moduleList.size()));
        log(String.format("Modules have %d different licenses", licenseMap.size()));

        log("Writing: " + catalogXmlPath);
        writeCatalogXml(moduleList, licenseMap);
        log("Writing: " + catalogXmlGzPath);
        createGZipCatalog(catalogXmlPath, catalogXmlGzPath);
        String msg = "DONE";
        log(msg);
    }

    private void createGZipCatalog(Path catalogXmlPath, Path catalogXmlGzPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(catalogXmlPath);
             GZIPOutputStream gzOutStream = new GZIPOutputStream(Files.newOutputStream(catalogXmlGzPath))) {
            byte[] buf = new byte[1024 * 50];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                gzOutStream.write(buf, 0, len);
            }
        }
    }

    private void writeCatalogXml(ArrayList<Node> moduleList, HashMap<String, Node> licenseMap) throws Exception {
        BufferedWriter writer = Files.newBufferedWriter(catalogXmlPath);
        writer.write(XML_HEAD);
        if (notificationMessage != null) {
            writeNotificationElement(writer);
        }
        writer.write(String.format("<module_updates timestamp=\"%s\">\r\n\r\n", timeStampFormat.format(Date.from(Instant.now()))));
        writeNodeCollection(moduleList, writer);
        writeNodeCollection(licenseMap.values(), writer);
        writer.write("</module_updates>\r\n");
        writer.close();
    }

    private void writeNotificationElement(BufferedWriter writer) throws IOException {
        String urlString = "";
        if (notificationURL != null) {
            urlString = String.format(" url=\"%s\"", notificationURL);
        }
        writer.write(String.format("<notification%s>%s</notification>\r\n\r\n", urlString, notificationMessage));
    }

    private boolean isNbmModuleFile(Path path) {
        return Files.isReadable(path) && path.getFileName().toString().endsWith(NBM_FILE_EXTENSION);
    }

    private void processModule(Path path, ArrayList<Node> moduleList, HashMap<String, Node> licenseMap) {
        try {
            log("Processing file: " + path.getFileName());
            final FileSystem nbmFileSystem = FileSystems.newFileSystem(path, null);
            final Path infoFile = nbmFileSystem.getPath(PATH_INFO_XML);
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            // This disables resolving the DTD given in the info.xml file.
            // It is not necessary to resolve it and it boosts the execution performance.
            documentBuilder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            final Document document = documentBuilder.parse(Files.newInputStream(infoFile));
            final Node moduleElement = document.getElementsByTagName(TAG_NAME_MODULE).item(0);
            updateDownloadSizeAttribute(moduleElement, Files.size(path));
            moduleList.add(moduleElement);
            Node licenseElement = getLicenseElement(moduleElement);
            if (licenseElement != null) {
                moduleElement.removeChild(licenseElement);
                addLicenseToMap(licenseElement, licenseMap);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void addLicenseToMap(Node licenseElement, HashMap<String, Node> licenseMap) {
        final NamedNodeMap attributes = licenseElement.getAttributes();
        if (attributes != null) {
            licenseMap.put(attributes.getNamedItem(ATTRIB_NAME_NAME).getNodeValue(), licenseElement);
        }
    }

    private void writeNodeCollection(Collection<Node> nodeCollection, BufferedWriter writer) throws Exception {
        for (Node node : nodeCollection) {
            StringWriter stringWriter = new StringWriter();
            xmlTransformer.transform(new DOMSource(node), new StreamResult(stringWriter));
            stringWriter.flush();
            writer.write(stringWriter.toString());
            writer.write("\r\n");
        }
    }

    private static void updateDownloadSizeAttribute(Node moduleElement, long sizeInBytes) {
        NamedNodeMap attributes = moduleElement.getAttributes();
        if (attributes == null) {
            return;
        }
        Node sizeItem = attributes.getNamedItem(ATTRIB_NAME_DOWNLOADSIZE);
        if (sizeItem == null) {
            return;
        }
        sizeItem.setNodeValue(String.valueOf(sizeInBytes));
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private static void logErr(String msg) {
        System.err.println(msg);
    }

    private static Node getLicenseElement(Node moduleElement) {
        final NodeList childNodes = moduleElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node item = childNodes.item(i);
            if (item.getNodeName().equals(TAG_NAME_LICENSE)) {
                return item;
            }
        }
        return null;
    }

}
