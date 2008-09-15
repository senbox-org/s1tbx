package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Format;
import static com.bc.ceres.binio.util.TypeBuilder.*;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines the formats of all supported SOMS product types.
 */
public class SmosFormats {
    public static final CompoundType SNAPSHOT_INFO_TYPE =
            COMP("Snapshot_Information",
                 MEMBER("Snapshot_Time", SEQ(UINT, 3)),
                 MEMBER("Snapshot_ID", UINT),
                 MEMBER("Snapshot_OBET", SEQ(UBYTE, 8)),
                 MEMBER("Position", SEQ(DOUBLE, 3)),
                 MEMBER("Velocity", SEQ(DOUBLE, 3)),
                 MEMBER("Vector_Source", UBYTE),
                 MEMBER("Q0", DOUBLE),
                 MEMBER("Q1", DOUBLE),
                 MEMBER("Q2", DOUBLE),
                 MEMBER("Q3", DOUBLE),
                 MEMBER("TEC", DOUBLE),
                 MEMBER("Geomag_F", DOUBLE),
                 MEMBER("Geomag_D", DOUBLE),
                 MEMBER("Geomag_I", DOUBLE),
                 MEMBER("Sun_RA", FLOAT),
                 MEMBER("Sun_DEC", FLOAT),
                 MEMBER("Sun_BT", FLOAT),
                 MEMBER("Accuracy", FLOAT),
                 MEMBER("Radiometric_Accuracy", SEQ(FLOAT, 2)));

    public static final CompoundType D1C_BT_DATA_TYPE =
            COMP("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Incidence_Angle", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Faraday_Rotation_Angle", USHORT),
                 MEMBER("Geometric_Rotation_Angle", USHORT),
                 MEMBER("Snapshot_ID_of_Pixel", UINT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType F1C_BT_DATA_TYPE =
            COMP("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value_Real", FLOAT),
                 MEMBER("BT_Value_Imag", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Incidence_Angle", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Faraday_Rotation_Angle", USHORT),
                 MEMBER("Geometric_Rotation_Angle", USHORT),
                 MEMBER("Snapshot_ID_of_Pixel", UINT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType BROWSE_BT_DATA_TYPE =
            COMP("Bt_Data",
                 MEMBER("Flags", USHORT),
                 MEMBER("BT_Value", FLOAT),
                 MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                 MEMBER("Azimuth_Angle", USHORT),
                 MEMBER("Footprint_Axis1", USHORT),
                 MEMBER("Footprint_Axis2", USHORT));

    public static final CompoundType D1C_GRID_POINT_DATA_TYPE =
            COMP("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", SEQ(D1C_BT_DATA_TYPE)));

    public static final CompoundType F1C_GRID_POINT_DATA_TYPE =
            COMP("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", SEQ(F1C_BT_DATA_TYPE)));

    public static final CompoundType BROWSE_GRID_POINT_DATA_TYPE =
            COMP("Grid_Point_Data",
                 MEMBER("Grid_Point_ID", UINT), /*4*/
                 MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                 MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                 MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                 MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                 MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                 MEMBER("BT_Data_List", SEQ(BROWSE_BT_DATA_TYPE)));

    public static final CompoundType MIR_SCXD1C_TYPE =
            COMP("MIR_SCXD1C",
                 MEMBER("Snapshot_Counter", UINT),
                 MEMBER("Snapshot_List", SEQ(SNAPSHOT_INFO_TYPE)),
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", SEQ(D1C_GRID_POINT_DATA_TYPE)));

    public static final CompoundType MIR_SCXF1C_TYPE =
            COMP("MIR_SCXF1C",
                 MEMBER("Snapshot_Counter", UINT),
                 MEMBER("Snapshot_List", SEQ(SNAPSHOT_INFO_TYPE)),
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", SEQ(F1C_GRID_POINT_DATA_TYPE)));

    public static final CompoundType MIR_BROWSE_TYPE =
            COMP("Temp_Browse",
                 MEMBER("Grid_Point_Counter", UINT),
                 MEMBER("Grid_Point_List", SEQ(BROWSE_GRID_POINT_DATA_TYPE)));


    private static final SmosFormats INSTANCE = new SmosFormats();
    private final Map<String, Format> formatMap;

    private SmosFormats() {
        formatMap = new HashMap<String, Format>(17);
        registerSmosFormat("MIR_BWLD1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
        registerSmosFormat("MIR_BWLF1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
        registerSmosFormat("MIR_BWSD1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);
        registerSmosFormat("MIR_BWSF1C", MIR_BROWSE_TYPE, BROWSE_GRID_POINT_DATA_TYPE, true);

        registerSmosFormat("MIR_SCLD1C", MIR_SCXD1C_TYPE, D1C_GRID_POINT_DATA_TYPE, false);
        registerSmosFormat("MIR_SCLF1C", MIR_SCXF1C_TYPE, F1C_GRID_POINT_DATA_TYPE, false);
        registerSmosFormat("MIR_SCSD1C", MIR_SCXD1C_TYPE, D1C_GRID_POINT_DATA_TYPE, false);
        registerSmosFormat("MIR_SCSF1C", MIR_SCXF1C_TYPE, F1C_GRID_POINT_DATA_TYPE, false);
    }

    public static SmosFormats getInstance() {
        return INSTANCE;
    }

    public String[] getFormatNames() {
        final Set<String> names = formatMap.keySet();
        return names.toArray(new String[names.size()]);
    }

    public Format getFormat(String name) {
        return formatMap.get(name);
    }

    private void registerSmosFormat(String formatName, CompoundType type, CompoundType gridPointType, boolean browse) {
        final Format smosFormat = new Format(type);
        smosFormat.setName(formatName);
        smosFormat.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        if (!browse) {
            smosFormat.addSequenceElementCountResolver("Snapshot_List", "Snapshot_Counter");
        }
        smosFormat.addSequenceElementCountResolver("Grid_Point_List", "Grid_Point_Counter");
        smosFormat.addSequenceElementCountResolver(gridPointType,
                                                   "BT_Data_List", "BT_Data_Counter");
        formatMap.put(formatName, smosFormat);
    }

}
