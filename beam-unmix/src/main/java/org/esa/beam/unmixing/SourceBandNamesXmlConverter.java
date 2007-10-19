/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.unmixing;

import com.bc.ceres.binding.ConversionException;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.ParameterXmlConverter;

public class SourceBandNamesXmlConverter implements ParameterXmlConverter {
    private static final String TAG_SOURCE_BANDS = "sourceBands";
    private static final String TAG_BAND = "band";

    public Class<?> getValueType() {
        return String[].class;
    }

    public Xpp3Dom getTemplateDom() {
        return convertValueToDom(new String[]{"Name of band 1", "Name of band 2", "..."});
    }

    public Xpp3Dom convertValueToDom(Object value) {
        final String[] sourceBandNames = (String[]) value;
        final Xpp3Dom sourceBandsElem = new Xpp3Dom(TAG_SOURCE_BANDS);
        for (String sourceBandName : sourceBandNames) {
            final Xpp3Dom bandElem = new Xpp3Dom(TAG_BAND);
            bandElem.setValue(sourceBandName);
            sourceBandsElem.addChild(bandElem);
        }
        return sourceBandsElem;
    }

    public Object convertDomToValue(Xpp3Dom dom) throws ConversionException {
        Xpp3Dom sourceBandsElem = dom.getChild(TAG_SOURCE_BANDS);
        Xpp3Dom[] bandElem = sourceBandsElem.getChildren(TAG_BAND);
        String[] sourceBandNames = new String[bandElem.length];
        for (int i = 0; i < bandElem.length; i++) {
            sourceBandNames[i] = bandElem[i].getValue();
        }
        return sourceBandNames;
    }
}