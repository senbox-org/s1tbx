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
package org.esa.snap.core.gpf.internal;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

/**
 * Represents the application data for graphs.
 *
 * @author marcoz
 */
public class ApplicationData {

    private final String appId;
    private final XppDom data;

    /**
     * Constructs an Application Data object from the given Id and data objects.
     *
     * @param string
     * @param xpp3Dom
     */
    public ApplicationData(String string, XppDom xpp3Dom) {
        appId = string;
        data = xpp3Dom;
    }

    /**
     * Returns the Application ID.
     *
     * @return the appId
     */
    public String getId() {
        return appId;
    }

    /**
     * Returns the Application data as XppDom
     *
     * @return the data
     */
    public XppDom getData() {
        return data;
    }

    public static class AppConverter implements Converter {

        private static final String ID_ATTRIBUTE_NAME = "id";

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                            MarshallingContext context) {

            ApplicationData applicationData = (ApplicationData) source;
            writer.addAttribute(ID_ATTRIBUTE_NAME, applicationData.appId);
            XppDom[] children = applicationData.data.getChildren();
            for (XppDom child : children) {
                HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
                XppDomReader reader = new XppDomReader(child);
                copier.copy(reader, writer);
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader,
                                UnmarshallingContext context) {
            HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
            XppDomWriter xppDomWriter = new XppDomWriter();
            String appId = reader.getAttribute(ID_ATTRIBUTE_NAME);
            copier.copy(reader, xppDomWriter);
            XppDom xpp3Dom = xppDomWriter.getConfiguration();
            return new ApplicationData(appId, xpp3Dom);
        }

        @Override
        public boolean canConvert(Class type) {
            return ApplicationData.class.equals(type);
        }
    }
}
