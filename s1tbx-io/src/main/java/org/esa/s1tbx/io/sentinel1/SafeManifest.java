/*
 * Copyright (C) 2023 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.sentinel1;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.text.DateFormat;

import static org.esa.snap.engine_utilities.datamodel.AbstractMetadata.NO_METADATA_STRING;

public class SafeManifest {

    public void addManifestMetadata(final String productName, final MetadataElement absRoot,
                                    final MetadataElement origProdRoot, boolean isOCN) {
        final String defStr = NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement XFDU = origProdRoot.getElement("XFDU");
        final MetadataElement informationPackageMap = XFDU.getElement("informationPackageMap");
        final MetadataElement contentUnit = informationPackageMap.getElement("contentUnit");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        final String descriptor = contentUnit.getAttributeString("textInfo", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, descriptor);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, "right");

        final MetadataElement metadataSection = XFDU.getElement("metadataSection");
        final MetadataElement[] metadataObjectList = metadataSection.getElements();
        final DateFormat sentinelDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd_HH:mm:ss");

        for (MetadataElement metadataObject : metadataObjectList) {
            final String id = metadataObject.getAttributeString("ID", defStr);
            if (id.endsWith("Annotation") || id.endsWith("Schema")) {
                // continue;
            } else if (id.equals("processing")) {
                final MetadataElement processing = findElement(metadataObject, "processing");
                final MetadataElement facility = processing.getElement("facility");
                final MetadataElement software = facility.getElement("software");
                final String org = facility.getAttributeString("organisation");
                final String name = software.getAttributeString("name");
                final String version = software.getAttributeString("version");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, org + ' ' + name + ' ' + version);

                final ProductData.UTC start = getTime(processing, "start", sentinelDateFormat);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, start);
            } else if (id.equals("acquisitionPeriod")) {
                final MetadataElement acquisitionPeriod = findElement(metadataObject, "acquisitionPeriod");
                final ProductData.UTC startTime = getTime(acquisitionPeriod, "startTime", sentinelDateFormat);
                final ProductData.UTC stopTime = getTime(acquisitionPeriod, "stopTime", sentinelDateFormat);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

            } else if (id.equals("platform")) {
                final MetadataElement platform = findElement(metadataObject, "platform");
                String missionName = platform.getAttributeString("familyName", "Sentinel-1");
                final String number = platform.getAttributeString("number", defStr);
                if (!missionName.equals("ENVISAT"))
                    missionName += number;
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, missionName);

                final MetadataElement instrument = platform.getElement("instrument");
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SWATH, instrument.getAttributeString("swath", defStr));
                String acqMode = instrument.getAttributeString("mode", defStr);
                if (acqMode == null || acqMode.equals(defStr)) {
                    final MetadataElement extensionElem = instrument.getElement("extension");
                    if (extensionElem != null) {
                        final MetadataElement instrumentModeElem = extensionElem.getElement("instrumentMode");
                        if (instrumentModeElem != null)
                            acqMode = instrumentModeElem.getAttributeString("mode", defStr);
                    }
                }
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, acqMode);
            } else if (id.equals("measurementOrbitReference")) {
                final MetadataElement orbitReference = findElement(metadataObject, "orbitReference");
                if(orbitReference != null) {
                    final MetadataElement orbitNumber = findElementContaining(orbitReference, "OrbitNumber", "type", "start");
                    if (orbitNumber != null) {
                        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, orbitNumber.getAttributeInt("orbitNumber", defInt));
                    }
                    final MetadataElement relativeOrbitNumber = findElementContaining(orbitReference, "relativeOrbitNumber", "type", "start");
                    if (relativeOrbitNumber != null) {
                        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, relativeOrbitNumber.getAttributeInt("relativeOrbitNumber", defInt));
                    }
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, orbitReference.getAttributeInt("cycleNumber", defInt));

                    String pass = orbitReference.getAttributeString("pass", defStr);
                    if (pass.equals(defStr)) {
                        MetadataElement extension = orbitReference.getElement("extension");
                        if(extension != null) {
                            final MetadataElement orbitProperties = extension.getElement("orbitProperties");
                            pass = orbitProperties.getAttributeString("pass", defStr);
                        }
                    }
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, pass);
                }
            } else if (id.equals("measurementFrameSet")) {

            } else if (id.equals("generalProductInformation")) {
                MetadataElement generalProductInformation = findElement(metadataObject, "generalProductInformation");
                if (generalProductInformation == null)
                    generalProductInformation = findElement(metadataObject, "standAloneProductInformation");

                String productType = "unknown";
                if (isOCN) {
                    productType = "OCN";
                } else {
                    if (generalProductInformation != null)
                        productType = generalProductInformation.getAttributeString("productType", defStr);
                }
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
                if (productType.contains("SLC")) {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "COMPLEX");
                } else {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
                }
            }
        }
    }

    private static MetadataElement findElement(final MetadataElement elem, final String name) {
        if(elem.containsElement("metadataWrap")) {
            final MetadataElement metadataWrap = elem.getElement("metadataWrap");
            if(metadataWrap.containsElement("xmlData")) {
                final MetadataElement xmlData = metadataWrap.getElement("xmlData");
                return xmlData.getElement(name);
            }
        }
        if(elem.containsElement("xmlData")) {
            final MetadataElement xmlData = elem.getElement("xmlData");
            return xmlData.getElement(name);
        }
        return null;
    }

    private static MetadataElement findElementContaining(final MetadataElement parent, final String elemName,
                                                         final String attribName, final String attValue) {
        final MetadataElement[] elems = parent.getElements();
        for (MetadataElement elem : elems) {
            if (elem.getName().equalsIgnoreCase(elemName) && elem.containsAttribute(attribName)) {
                String value = elem.getAttributeString(attribName);
                if (value != null && value.equalsIgnoreCase(attValue))
                    return elem;
            }
        }
        return null;
    }

    private static ProductData.UTC getTime(final MetadataElement elem, final String tag, final DateFormat sentinelDateFormat) {

        String start = elem.getAttributeString(tag, NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, sentinelDateFormat);
    }
}
