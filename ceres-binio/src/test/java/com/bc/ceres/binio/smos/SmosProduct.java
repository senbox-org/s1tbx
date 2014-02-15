/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binio.smos;

import com.bc.ceres.binio.*;
import org.junit.*;

import javax.imageio.stream.*;
import java.io.*;
import java.nio.*;

import static com.bc.ceres.binio.TypeBuilder.*;

public class SmosProduct {
    public static final int[] BT_DATA_COUNTERS = new int[]{2, 4, 3, 6};
    public static final CompoundType SNAPSHOT_INFO_TYPE =
            COMPOUND("Snapshot_Information",
                     MEMBER("Snapshot_Time", SEQUENCE(UINT, 3)),
                     MEMBER("Snapshot_ID", UINT),
                     MEMBER("Snapshot_OBET", SEQUENCE(UBYTE, 8)),
                     MEMBER("Position", SEQUENCE(DOUBLE, 3)),
                     MEMBER("Velocity", SEQUENCE(DOUBLE, 3)),
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
                     MEMBER("Radiometric_Accuracy", SEQUENCE(FLOAT, 2)));
    public static final CompoundType F1C_BT_DATA_TYPE =
            COMPOUND("Bt_Data",
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
    public static final CompoundType F1C_GRID_POINT_DATA_TYPE =
            COMPOUND("Grid_Point_Data",
                     MEMBER("Grid_Point_ID", UINT), /*4*/
                     MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                     MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                     MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                     MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                     MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                     MEMBER("Bt_Data_List", VAR_SEQUENCE(F1C_BT_DATA_TYPE, "BT_Data_Counter")));
    public static final CompoundType MIR_SCLF1C_TYPE =
            COMPOUND("MIR_SCLF1C",
                     MEMBER("Snapshot_Counter", UINT),
                     MEMBER("Snapshot_List", VAR_SEQUENCE(SNAPSHOT_INFO_TYPE, "Snapshot_Counter")),
                     MEMBER("Grid_Point_Counter", UINT),
                     MEMBER("Grid_Point_List", VAR_SEQUENCE(F1C_GRID_POINT_DATA_TYPE, "Grid_Point_Counter")));

    public static final DataFormat MIR_SCLF1C_FORMAT;

    static {
        MIR_SCLF1C_FORMAT = new DataFormat(SmosProduct.MIR_SCLF1C_TYPE, ByteOrder.LITTLE_ENDIAN);
    }


    public static byte[] createTestProductData(ByteOrder byteOrder) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(baos);

        int size = 4;   // uint Snapshot_Counter
        size += 3 * SNAPSHOT_INFO_TYPE.getSize();  // Snapshot_Info[3] Snapshot_List
        size += 4;      // uint Grid_Point_Counter
        for (int btDataCount : BT_DATA_COUNTERS) {  // Grid_Point_Data[4] Grid_Point_List
            size += 18;
            size += btDataCount * F1C_BT_DATA_TYPE.getSize(); // Bt_Data[btDataCount] Bt_Data_List
        }

        ios.setByteOrder(byteOrder);
        ios.writeInt(3);      // uint Snapshot_Counter
        for (int i = 0; i < 3; i++) {
            int[] snapshotTime = getSnapshotTime(i);
            ios.writeInt(snapshotTime[0]);   // uint Snapshot_Info[i].Snapshot_Time[0]
            ios.writeInt(snapshotTime[1]);   // uint Snapshot_Info[i].Snapshot_Time[1]
            ios.writeInt(snapshotTime[2]);   // uint Snapshot_Info[i].Snapshot_Time[2]
            ios.writeInt(getSnapshotId(i));  // uint Snapshot_Info[i].Snapshot_ID
            ios.skipBytes(SNAPSHOT_INFO_TYPE.getSize() - (3 * 4 + 4));
        }
        ios.writeInt(BT_DATA_COUNTERS.length);   // uint Grid_Point_Counter
        for (int i = 0; i < BT_DATA_COUNTERS.length; i++) {
            int btDataCounter = BT_DATA_COUNTERS[i];
            ios.writeInt(getGridPointId(i));          // uint   Grid_Point_Data[i].Grid_Point_ID
            ios.writeFloat(getGridPointLatitude(i));  // float  Grid_Point_Data[i].Grid_Point_Latitude
            ios.writeFloat(getGridPointLongitude(i)); // float  Grid_Point_Data[i].Grid_Point_Longitude
            ios.skipBytes(18 - (4 + 2 * 4 + 1));
            ios.writeByte(btDataCounter);             // ubyte  Grid_Point_Data[i].BT_Data_Counter
            for (int j = 0; j < btDataCounter; j++) {
                ios.writeShort(getBtDataFlags(j));    // ushort Bt_Data[j].Flags
                ios.writeFloat(getBtValueReal(j));    // float  Bt_Data[j].BT_Value_Real
                ios.writeFloat(getBtValueImag(j));    // float  Bt_Data[j].BT_Value_Imag
                ios.skipBytes(F1C_BT_DATA_TYPE.getSize() - (2 + 2 * 4 + 2));
                ios.writeShort(getFootprintAxis2(j)); // ushort Bt_Data[j].Footprint_Axis2
            }
        }
        ios.close();

        final byte[] bytes = baos.toByteArray();
        Assert.assertEquals(size, bytes.length);
        return bytes;
    }

    public static int[] getSnapshotTime(int i) {
        return new int[]{2540, i, 10000 + i};
    }

    public static int getSnapshotId(int i) {
        return 40000 + i;
    }

    public static int getGridPointId(int i) {
        return 968000 + i;
    }

    public static float getGridPointLatitude(int i) {
        return 36.0F + i * 0.1F;
    }

    public static float getGridPointLongitude(int i) {
        return 13.0F + i * 0.1F;
    }

    public static int getBtDataFlags(int j) {
        return 1 + j * 2;
    }

    public static float getBtValueReal(int j) {
        return 0.5F * (j % 2);
    }

    public static float getBtValueImag(int j) {
        return 0.5F * ((1 - j) % 2);
    }

    public static int getFootprintAxis2(int j) {
        return j % 3;
    }

}
