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

package org.esa.beam.dataio.avhrr.binio;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A record of the NOAA AVHRR file base on a bin-io type
 */
public class Record {

    private final CompoundData recordData;
    private final boolean isAscii;

    public Record(CompoundData recordData, boolean isAscii) {
        this.recordData = recordData;
        this.isAscii = isAscii;
    }

     public int getMemberIndex(String memberName) {
        return recordData.getMemberIndex(memberName);
    }

    public String getDescription(int memberIndex) {
        FormatMetadata metaData = getMetaData(memberIndex);
        return  metaData.getDescription();
    }

    public String getUnits(int memberIndex) {
        FormatMetadata metaData =  getMetaData(memberIndex);
        return metaData.getUnits();
    }

    public String getString(int memberIndex) throws IOException {
        return getRawString(memberIndex).trim();
    }

    public int getInt(int memberIndex) throws IOException {
        ProductData productData = getProductData(memberIndex);
        if (productData.isInt()) {
            return productData.getElemInt();
        }
        throw new IllegalArgumentException("Member ''"+memberIndex+"'' is not of type integer.");
    }

    public ProductData getProductData(int memberIndex) throws IOException {
        if (isAscii) {
            return getProductDataAscii(memberIndex);
        } else {
            return getProductDataBinary(memberIndex);
        }
    }

    public ProductData getProductDataAscii(int memberIndex) throws IOException {
        String valueAsString = getRawString(memberIndex).trim();
        FormatMetadata metaData = getMetaData(memberIndex);
        String type = metaData.getType();
        String scalingFactor = metaData.getScalingFactor();
        if (type.equals("string")) {
            return ProductData.createInstance(valueAsString);
        } else if (type.equals("enumerated")) {
            return ProductData.createInstance(metaData.getItems().get(valueAsString));
        } else if (type.equals("time")) {
            return convertStringToDate(valueAsString, "yyyyMMddHHmmss'Z'");
        } else if (type.equals("longtime")) {
            return convertStringToDate(valueAsString, "yyyyMMddHHmmssSSS'Z'");
        } else if (type.equals("integer") || type.equals("uinteger")) {
            long longValue = Long.parseLong(valueAsString);
            if (scalingFactor != null && !scalingFactor.isEmpty()) {
                int powerIndex = scalingFactor.indexOf('^');
                String scaling = scalingFactor.substring(powerIndex+1);
                int intScale = Integer.parseInt(scaling);
                double doubleValue = longValue / Math.pow(10, intScale);
                return ProductData.createInstance(new double[]{doubleValue});
            } else {
                return ProductData.createInstance(new long[]{longValue});
            }
        }
        return ProductData.createInstance(valueAsString);
    }

    private ProductData convertStringToDate(String dateString, String dateFormatString) {
        DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        try {
            final Date date = dateFormat.parse(dateString);
            return ProductData.UTC.create(date, 0);
        } catch (ParseException e) {
            return ProductData.createInstance(dateString);
        }
    }

    public ProductData getProductDataBinary(int memberIndex) throws IOException {

        FormatMetadata metaData = getMetaData(memberIndex);
        String type = metaData.getType();
        String scalingFactor = metaData.getScalingFactor();
        if (recordData.getType().getMemberType(memberIndex).isSequenceType() &&
                !type.equals("string")) {
            return ProductData.createInstance("TODO array_type");
        }
        if (type.equals("string")) {
            return ProductData.createInstance(getAsString(recordData.getSequence(memberIndex)));
        } else if (type.equals("enumerated")) {
//            return ProductData.createInstance(metaData.getItems().get(valueAsString));
        } else if (type.equals("time") || type.equals("short_cds_time")) {
//            EpsFile.readShortCdsTime(recordData.getCompound(memberIndex));
            return ProductData.createInstance("converted_long_time");
        } else if (type.startsWith("integer") || type.startsWith("uinteger")) {
            int intValue = recordData.getInt(memberIndex);
//            long longValue = Long.parseLong(valueAsString);
            if (scalingFactor != null && !scalingFactor.isEmpty() && !scalingFactor.contains(",")) {
                int powerIndex = scalingFactor.indexOf('^');
                String scaling = scalingFactor.substring(powerIndex+1);
                int intScale = Integer.parseInt(scaling);
                double doubleValue = intValue / Math.pow(10, intScale);
                return ProductData.createInstance(new double[]{doubleValue});
            } else {
                return ProductData.createInstance(new int[]{intValue});
            }
        }
        return ProductData.createInstance("TODO "+type);
    }

    public MetadataElement getAsMetaDataElement() throws IOException {
        CompoundType type = recordData.getType();
        final int memberCount = type.getMemberCount();
        MetadataElement metadataElement = new MetadataElement(getRecordName());
        for (int i = 0; i < memberCount; i++) {
            String name = type.getMemberName(i);
            ProductData data = getProductData(i);
            MetadataAttribute attribute = new MetadataAttribute(name, data, true);
            attribute.setDescription(getDescription(i));
            attribute.setUnit(getUnits(i));
            metadataElement.addAttribute(attribute);
        }
        return metadataElement;
    }

    private String getRecordName() {
        return recordData.getType().getName();
    }

    private String getAsString(SequenceData valueSequence) throws IOException {
        byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data).trim();
    }

    private String getRawString(int memberIndex) throws IOException {
        CompoundData fieldData = recordData.getCompound(memberIndex);
        return getAsString(fieldData.getSequence("value"));
    }

    // debug
    void printMemberNames() {
        CompoundMember[] members = recordData.getType().getMembers();
        for (CompoundMember compoundMember : members) {
            System.out.println(compoundMember.getName());
        }
    }

    private FormatMetadata getMetaData(int memberIndex) {
        CompoundType compoundType = recordData.getType();
        CompoundMember member = compoundType.getMember(memberIndex);
        Object object =  member.getMetadata();
        if (object != null && object instanceof FormatMetadata) {
            return (FormatMetadata) object;
        } else {
            return new FormatMetadata();
        }
    }

}
