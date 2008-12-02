package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.binx.BinX;
import com.bc.ceres.binio.binx.BinXException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Defines the formats of all supported SMOS product types.
 */
public class SmosFormats {

    private static final SmosFormats INSTANCE = new SmosFormats();

    private final ConcurrentMap<String, DataFormat> formatMap;

    private SmosFormats() {
        formatMap = new ConcurrentHashMap<String, DataFormat>(17);
    }

    public static SmosFormats getInstance() {
        return INSTANCE;
    }

    public String[] getFormatNames() {
        final Set<String> names = formatMap.keySet();
        return names.toArray(new String[names.size()]);
    }

    public DataFormat getFormat(String name) {
        if (!formatMap.containsKey(name)) {
            final URL schemaUrl = getSchemaResource(name);

            if (schemaUrl != null) {
                final BinX binX = createBinX();

                try {
                    final DataFormat format = binX.readDataFormat(schemaUrl.toURI(), name);
                    format.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    formatMap.putIfAbsent(name, format);
                } catch (BinXException e) {
                    throw new IllegalStateException(
                            MessageFormat.format("Schema resource ''{0}'': {1}", schemaUrl, e.getMessage()));
                } catch (IOException e) {
                    throw new IllegalStateException(
                            MessageFormat.format("Schema resource ''{0}'': {1}", schemaUrl, e.getMessage()));
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(
                            MessageFormat.format("Schema resource ''{0}'': {1}", schemaUrl, e.getMessage()));
                }
            }
        }

        return formatMap.get(name);
    }

    static DataFormat getFormat(File hdrFile) throws IOException {
        final Document document;

        try {
            document = new SAXBuilder().build(hdrFile);
        } catch (JDOMException e) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Invalid document", hdrFile.getPath()), e);
        }

        final Namespace namespace = document.getRootElement().getNamespace();
        if (namespace == null) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Missing namespace", hdrFile.getPath()));
        }

        final Element variableHeader = document.getRootElement().getChild("Variable_Header", namespace);
        if (variableHeader == null) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Missing variable header", hdrFile.getPath()));
        }

        final Element specificProductHeader = variableHeader.getChild("Specific_Product_Header", namespace);
        if (specificProductHeader == null) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Missing specific product header", hdrFile.getPath()));
        }

        final Element mainInfo = specificProductHeader.getChild("Main_Info", namespace);
        if (mainInfo == null) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Missing main info.", hdrFile.getPath()));
        }

        final String schema = mainInfo.getChildText("Datablock_Schema", namespace);
        if (schema == null) {
            throw new IOException(
                    MessageFormat.format("File ''{0}'': Missing datablock schema''", hdrFile.getPath()));
        }

        return getInstance().getFormat(schema);
    }

    static URL getSchemaResource(String name) {
        // Reference: SO-MA-IDR-GS-0004, SMOS DPGS, XML Schema Guidelines
        if (name == null || !name.matches("DBL_\\w{2}_\\w{4}_\\w{10}_\\d{4}(\\.binXschema\\.xml)?")) {
            return null;
        }

        final String fc = name.substring(12, 16);
        final String sd = name.substring(16, 22);

        final StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("schemas/").append(fc).append("/").append(sd).append("/").append(name);
        if (!name.endsWith(".binXschema.xml")) {
            nameBuilder.append(".binXschema.xml");
        }

        return SmosFormats.class.getResource(nameBuilder.toString());
    }

    private static BinX createBinX() {
        final BinX binX = new BinX();
        binX.setSingleDatasetStructInlined(true);
        binX.setArrayVariableInlined(true);

        try {
            final Properties properties = new Properties();
            final InputStream is = SmosFormats.class.getResourceAsStream("binx_var_name.properties");

            if (is != null) {
                properties.load(is);
                binX.setVarNameMappings(properties);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

        return binX;
    }
}
