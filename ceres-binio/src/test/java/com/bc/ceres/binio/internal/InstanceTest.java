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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;
import static com.bc.ceres.binio.TypeBuilder.*;
import com.bc.ceres.binio.smos.SmosProduct;
import com.bc.ceres.binio.util.ByteArrayIOHandler;
import com.bc.ceres.binio.util.ImageIOHandler;
import junit.framework.TestCase;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;


public class InstanceTest extends TestCase {

    public void testGeneratedInstanceTypes() throws IOException {
        final byte[] byteData = SmosProduct.createTestProductData(SmosProduct.MIR_SCLF1C_FORMAT.getByteOrder());
        final TracingIOHandler ioHandler = new TracingIOHandler(new ByteArrayIOHandler(byteData));
        final DataContext context = SmosProduct.MIR_SCLF1C_FORMAT.createContext(ioHandler);

        final CompoundData mirSclf1cData = context.getData();
        final SequenceData snapshotList = mirSclf1cData.getSequence("Snapshot_List");
        final CompoundData snapshotData = snapshotList.getCompound(0);
        final SequenceData gridPointList = mirSclf1cData.getSequence("Grid_Point_List");
        final CompoundData gridPointData = gridPointList.getCompound(0);
        final SequenceData btDataList = gridPointData.getSequence("Bt_Data_List");
        final CompoundData btData = btDataList.getCompound(0);

        assertSame(VarCompound.class, mirSclf1cData.getClass());
        assertSame(FixSequenceOfFixCollections.class, snapshotList.getClass());
        assertSame(FixCompound.class, snapshotData.getClass());
        assertSame(FixSequenceOfVarCollections.class, gridPointList.getClass());
        assertSame(VarCompound.class, gridPointData.getClass());
        assertSame(FixSequenceOfFixCollections.class, btDataList.getClass());
        assertSame(FixCompound.class, btData.getClass());
    }


    public void testFixSequenceOfSimples() throws IOException {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        ios.writeInt(2134);
        ios.writeInt(45);
        ios.writeInt(36134);
        ios.close();

        final ImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final DataContext context = new DataFormat(COMPOUND("UNDEFINED"), ByteOrder.LITTLE_ENDIAN).createContext(
                new ImageIOHandler(iis));

        SequenceType type = SEQUENCE(SimpleType.INT, 3);
        final FixSequenceOfSimples sequenceInstance = new FixSequenceOfSimples(context, null, type, 0);

        assertEquals(3, sequenceInstance.getElementCount());
        assertEquals(3 * 4, sequenceInstance.getSize());
        assertEquals(false, sequenceInstance.isDataAccessible());

        sequenceInstance.makeDataAccessible();

        assertEquals(true, sequenceInstance.isDataAccessible());
        assertEquals(2134, sequenceInstance.getInt(0));
        assertEquals(45, sequenceInstance.getInt(1));
        assertEquals(36134, sequenceInstance.getInt(2));
    }

    public void testFixCompound() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        ios.writeInt(33);
        ios.writeInt(55);
        ios.writeFloat(27.88f);
        ios.close();

        CompoundType type = COMPOUND("compoundTestType",
                                     MEMBER("a", SimpleType.UINT),
                                     MEMBER("b", SimpleType.FLOAT));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 7));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 8));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 9));

        final byte[] byteData = baos.toByteArray();
        assertEquals(3 * 4, byteData.length);
        final DataContext context = new DataFormat(type, ByteOrder.LITTLE_ENDIAN).createContext(
                new ByteArrayIOHandler(byteData));

        CompoundInstance compoundInstance = InstanceFactory.createCompound(context, null, type, 4,
                                                                           ByteOrder.LITTLE_ENDIAN);
        assertSame(FixCompound.class, compoundInstance.getClass());

        assertEquals(2, compoundInstance.getElementCount());
        assertEquals(2 * 4, compoundInstance.getSize());
        assertEquals(4, compoundInstance.getPosition());
        assertEquals(type, compoundInstance.getType());
        assertEquals(true, compoundInstance.isSizeResolved());
        assertEquals(55, compoundInstance.getInt(0));
        assertEquals(27.88f, compoundInstance.getFloat(1), 0.00001f);
    }

    public void testFixCompoundOfFixCompounds() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        ios.writeDouble(17.0);
        ios.writeDouble(11.0);
        ios.writeDouble(19.0);
        ios.writeDouble(67.0);
        ios.close();

        final CompoundType complexType = COMPOUND("Complex", MEMBER("x", DOUBLE), MEMBER("y", DOUBLE));
        CompoundType type = COMPOUND("compoundTestType",
                                     MEMBER("x", SimpleType.DOUBLE),
                                     MEMBER("y", SimpleType.DOUBLE),
                                     MEMBER("z", complexType));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 31));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 32));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 33));

        final byte[] byteData = baos.toByteArray();
        assertEquals(2 * 8 + 16, byteData.length);
        final DataContext context = new DataFormat(type, ByteOrder.LITTLE_ENDIAN).createContext(
                new ByteArrayIOHandler(byteData));

        CompoundInstance compoundInstance = InstanceFactory.createCompound(context, null, type, 0,
                                                                           ByteOrder.LITTLE_ENDIAN);
        assertSame(FixCompound.class, compoundInstance.getClass());

        assertEquals(3, compoundInstance.getElementCount());
        assertEquals(2 * 8 + 16, compoundInstance.getSize());
        assertEquals(0, compoundInstance.getPosition());
        assertEquals(type, compoundInstance.getType());
        assertEquals(true, compoundInstance.isSizeResolved());
        assertEquals(11.0, compoundInstance.getDouble(1), 0.0);
        assertEquals(19.0, compoundInstance.getCompound(2).getDouble(0), 0.0);
        assertEquals(67.0, compoundInstance.getCompound(2).getDouble(1), 0.0);

        final CompoundData complexData = compoundInstance.getCompound(2);
        complexData.setDouble(0, 67.0);
        complexData.setDouble(0, 19.0);

        // todo - uncomment and make test run (rq-20091005)
        // compoundInstance.setCompound(2, complexData);
        // assertEquals(19.0, compoundInstance.getCompound(2).getDouble(0), 0.0);
        // assertEquals(67.0, compoundInstance.getCompound(2).getDouble(1), 0.0);
    }

    public void testVarCompound() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.BIG_ENDIAN);
        ios.writeInt(3);
        ios.writeDouble(111.1);
        ios.writeDouble(222.2);
        ios.writeDouble(333.3);
        ios.close();

        CompoundType type = COMPOUND("compoundTestType",
                                     MEMBER("count", SimpleType.INT),
                                     MEMBER("list", VAR_SEQUENCE(SimpleType.DOUBLE, "count")));

        DataFormat format = new DataFormat(type, ByteOrder.BIG_ENDIAN);
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 4));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 10));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 10000));

        final byte[] byteData = baos.toByteArray();
        assertEquals(4 + 3 * 8, byteData.length);
        final DataContext context = format.createContext(new ByteArrayIOHandler(byteData));

        CompoundData compoundData = context.getData();
        assertTrue(compoundData instanceof CompoundInstance);
        CompoundInstance compoundInstance = (CompoundInstance) compoundData;
        assertSame(VarCompound.class, compoundInstance.getClass());

        assertEquals(2, compoundInstance.getMemberCount());
        assertFalse(compoundInstance.isSizeResolved());

        SequenceData sequenceData = compoundInstance.getSequence(1);
        assertSame(FixSequenceOfSimples.class, sequenceData.getClass());

        compoundInstance.resolveSize();

        assertTrue(compoundInstance.isSizeResolved());
        assertTrue(compoundInstance.getSize() > 0);

        assertNotNull(sequenceData);
        assertEquals(3, sequenceData.getElementCount());
        assertEquals(111.1, sequenceData.getDouble(0), 1e-10);
        assertEquals(222.2, sequenceData.getDouble(1), 1e-10);
        assertEquals(333.3, sequenceData.getDouble(2), 1e-10);
    }

    public void testFixSequenceOfFixCollections() throws IOException {

        final int n = 11;
        final CompoundType type =
                COMPOUND("U",
                         MEMBER("A", INT),
                         MEMBER("B",
                                SEQUENCE(
                                        COMPOUND("P",
                                                 MEMBER("X", DOUBLE),
                                                 MEMBER("Y", DOUBLE)),
                                        n
                                )
                         ),
                         MEMBER("C", INT)
                );

        assertTrue(type.isSizeKnown());
        assertEquals(4 + n * (8 + 8) + 4, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(type.getSize());
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.writeInt(12345678);
        for (int i = 0; i < n; i++) {
            ios.writeDouble(20.0 + 0.1 * i);
            ios.writeDouble(40.0 + 0.1 * i);
        }
        ios.writeInt(87654321);
        ios.close();

        final byte[] byteData = baos.toByteArray();
        assertEquals(4 + n * (8 + 8) + 4, byteData.length);
        final DataContext context = new DataFormat(type).createContext(new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(FixCompound.class, compoundData.getClass());
        assertSame(FixSequenceOfFixCollections.class, compoundData.getSequence(1).getClass());
        assertSame(FixCompound.class, compoundData.getSequence(1).getCompound(0).getClass());

        assertEquals(12345678, compoundData.getInt(0));
        for (int i = 0; i < n; i++) {
            assertEquals("i=" + i, 20.0 + 0.1 * i, compoundData.getSequence(1).getCompound(i).getDouble(0), 1e-10);
            assertEquals("i=" + i, 40.0 + 0.1 * i, compoundData.getSequence(1).getCompound(i).getDouble(1), 1e-10);
        }
        assertEquals(87654321, compoundData.getInt(2));
    }

    public void testFixSequenceOfVarCollections() throws IOException {

        final int ni = 2;
        final int nj = 3;
        final CompoundType pointType = COMPOUND("Point", MEMBER("X", DOUBLE), MEMBER("Y", DOUBLE));
        final SequenceType seqType1 = _SEQ(pointType, ni);
        final SequenceType seqType2 = _SEQ(seqType1, nj);
        final CompoundType type = COMPOUND("C", MEMBER("M", seqType2));
        final DataFormat format = new DataFormat(type, ByteOrder.BIG_ENDIAN);
        assertFalse(type.isSizeKnown());
        assertEquals(-1, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(ni * nj * (8 + 8));
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(format.getByteOrder());
        for (int j = 0; j < nj; j++) {
            for (int i = 0; i < ni; i++) {
                ios.writeDouble(20.0 + 0.1 * i + 0.2 * j);
                ios.writeDouble(40.0 + 0.1 * i + 0.2 * j);
            }
        }
        ios.close();

        final byte[] byteData = baos.toByteArray();
        assertEquals(ni * nj * 2 * 8, byteData.length);
        final DataContext context = format.createContext(new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(VarCompound.class, compoundData.getClass());
        assertSame(FixSequenceOfVarCollections.class, compoundData.getSequence(0).getClass());
        assertSame(FixSequenceOfFixCollections.class, compoundData.getSequence(0).getSequence(0).getClass());
        assertSame(FixCompound.class, compoundData.getSequence(0).getSequence(0).getCompound(0).getClass());

        for (int j = 0; j < nj; j++) {
            for (int i = 0; i < ni; i++) {
                assertEquals("i=" + i + ",j=" + j, 20.0 + 0.1 * i + 0.2 * j,
                             compoundData.getSequence(0).getSequence(j).getCompound(i).getDouble(0), 1e-10);
                assertEquals("i=" + i + ",j=" + j, 40.0 + 0.1 * i + 0.2 * j,
                             compoundData.getSequence(0).getSequence(j).getCompound(i).getDouble(1), 1e-10);
            }
        }
    }

    public void testNestedFixSequenceOfVarCollections() throws IOException {

        final int ni = 4;
        final int nj = 2;
        final int nk = 3;
        final SequenceType seqType0 = _SEQ(DOUBLE, ni);
        final CompoundType pointType = COMPOUND("Point", MEMBER("Coords", seqType0));
        final SequenceType seqType1 = _SEQ(pointType, nj);
        final SequenceType seqType2 = _SEQ(seqType1, nk);
        final CompoundType type = COMPOUND("Polygon", MEMBER("PointList", seqType2));
        final DataFormat format = new DataFormat(type, ByteOrder.BIG_ENDIAN);

        assertFalse(type.isSizeKnown());
        assertEquals(-1, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(ni * nj * nk * 8);
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(format.getByteOrder());
        for (int k = 0; k < nk; k++) {
            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    ios.writeDouble(10.0 * i + 0.1 * j + 0.2 * k);
                }
            }
        }
        ios.close();
        final byte[] byteData = baos.toByteArray();
        assertEquals(ni * nj * nk * 8, byteData.length);
        final DataContext context = format.createContext(new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(VarCompound.class, compoundData.getClass());
        final SequenceData pointListData = compoundData.getSequence("PointList");
        assertSame(FixSequenceOfVarCollections.class, pointListData.getClass());
        final SequenceData pointListDataSeq0 = pointListData.getSequence(0);
        assertSame(FixSequenceOfVarCollections.class, pointListDataSeq0.getClass());
        final CompoundData pointListDataSeq0Comp0 = pointListDataSeq0.getCompound(0);
        assertSame(VarCompound.class, pointListDataSeq0Comp0.getClass());
        final SequenceData pointListDataSeq0Comp0Coords = pointListDataSeq0Comp0.getSequence("Coords");
        assertSame(FixSequenceOfSimples.class, pointListDataSeq0Comp0Coords.getClass());

        for (int k = 0; k < nk; k++) {
            final SequenceData kData = pointListData.getSequence(k);
            for (int j = 0; j < nj; j++) {
                final CompoundData kjData = kData.getCompound(j);
                final SequenceData coordsData = kjData.getSequence("Coords");
                for (int i = 0; i < ni; i++) {
                    assertEquals("i=" + i + ",j=" + j + ",k=" + k,
                                 10.0 * i + 0.1 * j + 0.2 * k,
                                 coordsData.getDouble(i), 1e-10);
                }
            }
        }
    }

    // create a pseudo VarSequenceType
    static VarSequenceType _SEQ(final Type elementType, final int elementCount) {
        return new VarElementCountSequenceType(elementType) {
            @Override
            protected int resolveElementCount(CollectionData parent) throws IOException {
                return elementCount;
            }
        };
    }
}