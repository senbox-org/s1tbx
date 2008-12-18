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
 * Registry for all supported SMOS product formats.
 */
public class SmosFormats {

    public static final String GRID_POINT_LIST_NAME = "Grid_Point_List";
    public static final String GRID_POINT_ID_NAME = "Grid_Point_ID";
    public static final String GRID_POINT_LATITUDE_NAME = "Grid_Point_Latitude";
    public static final String GRID_POINT_LONGITUDE_NAME = "Grid_Point_Longitude";

    public static final String SNAPSHOT_LIST_NAME = "Snapshot_List";
    public static final String SNAPSHOT_ID_NAME = "Snapshot_ID";

    public static final String BT_DATA_LIST_NAME = "BT_Data_List";
    public static final String BT_FLAGS_NAME = "Flags";
    public static final String BT_INCIDENCE_ANGLE_NAME = "Incidence_Angle";
    public static final String BT_SNAPSHOT_ID_OF_PIXEL_NAME = "Snapshot_ID_of_Pixel";

    public static final int L1C_POL_FLAGS_MASK = 3;
    public static final int L1C_POL_MODE_X = 0;
    public static final int L1C_POL_MODE_Y = 1;
    public static final int L1C_POL_MODE_XY1 = 2;
    public static final int L1C_POL_MODE_XY2 = 3;

    @SuppressWarnings({"PointlessBitwiseExpression"})
    public static final FlagDescriptor[] L1C_FLAGS = {
            new FlagDescriptor(1 << 0, "POL_FLAG_1", ""),
            new FlagDescriptor(1 << 1, "POL_FLAG_2", ""),
            new FlagDescriptor(1 << 2, "SUN_FOV", ""),
            new FlagDescriptor(1 << 3, "SUN_GLINT_FOV", ""),
            new FlagDescriptor(1 << 4, "MOON_GLINT_FOV", ""),
            new FlagDescriptor(1 << 5, "SINGLE_SNAPSHOT", ""),
            new FlagDescriptor(1 << 6, "FTT", ""),
            new FlagDescriptor(1 << 7, "SUN_POINT", ""),
            new FlagDescriptor(1 << 8, "SUN_GLINT_AREA", ""),
            new FlagDescriptor(1 << 9, "MOON_POINT", ""),
            new FlagDescriptor(1 << 10, "AF_FOV", ""),
            new FlagDescriptor(1 << 11, "EAF_FOV", ""),
            new FlagDescriptor(1 << 12, "BORDER_FOV", ""),
            new FlagDescriptor(1 << 13, "SUN_TAILS", ""),
            new FlagDescriptor(1 << 14, "RFI", ""),
    };

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
                final BinX binX = createBinX(name);

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

    private static BinX createBinX(String name) {
        final BinX binX = new BinX();
        binX.setSingleDatasetStructInlined(true);
        binX.setArrayVariableInlined(true);

        try {
            binX.setVarNameMappings(getResourceAsProperties("binx_var_name_mappings.properties"));

            if (name.contains("MIR_OSUDP2")) {
                binX.setTypeMembersInlined(getResourceAsProperties("binx_inlined_structs_MIR_OSUDP2.properties"));
            }
            if (name.contains("MIR_SMUDP2")) {
                binX.setTypeMembersInlined(getResourceAsProperties("binx_inlined_structs_MIR_SMUDP2.properties"));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

        return binX;
    }

    private static Properties getResourceAsProperties(String name) throws IOException {
        final Properties properties = new Properties();
        final InputStream is = SmosFormats.class.getResourceAsStream(name);

        if (is != null) {
            properties.load(is);
        }

        return properties;
    }
}
