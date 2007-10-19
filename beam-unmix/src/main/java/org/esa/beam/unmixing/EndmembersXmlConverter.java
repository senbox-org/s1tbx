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
import org.esa.beam.util.StringUtils;

public class EndmembersXmlConverter implements ParameterXmlConverter {
    private static final String TAG_ENDMEMBERS = "endmembers";
    private static final String TAG_ENDMEMBER = "endmember";
    private static final String TAG_NAME = "name";
    private static final String TAG_WAVELENGTHS = "wavelengths";
    private static final String TAG_RADIATIONS = "radiations";

    public Class<?> getValueType() {
        return Endmember[].class;
    }

    public Xpp3Dom getTemplateDom() {
        final Xpp3Dom endmembers = new Xpp3Dom(TAG_ENDMEMBERS);

        final Xpp3Dom wavelengths = new Xpp3Dom(TAG_WAVELENGTHS);
        wavelengths.setValue("Comma separated list of wavelength");
        endmembers.addChild(wavelengths);

        final Xpp3Dom endmember = new Xpp3Dom(TAG_ENDMEMBER);

        final Xpp3Dom name = new Xpp3Dom(TAG_NAME);
        name.setValue("Name of the endmember");
        endmember.addChild(name);

        final Xpp3Dom radiations = new Xpp3Dom(TAG_RADIATIONS);
        radiations.setValue("Comma separated list of radiations, size equal to #wavelengths)");
        endmember.addChild(radiations);

        endmembers.addChild(endmember);
        return endmembers;
    }

    public Xpp3Dom convertValueToDom(Object value) {
        final Endmember[] endmembers = (Endmember[]) value;
        final Xpp3Dom endmembersElem = new Xpp3Dom(TAG_ENDMEMBERS);
        for (Endmember endmember : endmembers) {
            final Xpp3Dom endmemberElem = new Xpp3Dom(TAG_ENDMEMBER);

            final Xpp3Dom nameElem = new Xpp3Dom(TAG_NAME);
            nameElem.setValue(endmember.getName());
            endmemberElem.addChild(nameElem);

            final Xpp3Dom wavelengthsElem = new Xpp3Dom(TAG_WAVELENGTHS);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < endmember.getSize(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(endmember.getWavelength(i));
            }
            wavelengthsElem.setValue(sb.toString());
            endmemberElem.addChild(wavelengthsElem);

            final Xpp3Dom radiationsElem = new Xpp3Dom(TAG_RADIATIONS);
            sb = new StringBuilder();
            for (int i = 0; i < endmember.getSize(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(endmember.getRadiation(i));
            }

            radiationsElem.setValue(sb.toString());
            endmemberElem.addChild(radiationsElem);
        }
        return endmembersElem;
    }

    public Object convertDomToValue(Xpp3Dom dom) throws ConversionException {
        Xpp3Dom endmembersElement = dom.getChild(TAG_ENDMEMBERS);
        Xpp3Dom wavelengthsElement = endmembersElement.getChild(TAG_WAVELENGTHS);
        if (wavelengthsElement == null) {
            throw new ConversionException("Missing 'endmembers/wavelengths' element.");
        }
        double[] wavelengths = StringUtils.toDoubleArray(wavelengthsElement.getValue(), ",");

        Xpp3Dom[] endmemberElements = endmembersElement.getChildren(TAG_ENDMEMBER);
        if (endmemberElements.length == 0) {
            throw new ConversionException("Missing 'endmembers/endmember' elements.");
        }
        Endmember[] endmembers = new Endmember[endmemberElements.length];
        for (int i = 0; i < endmemberElements.length; i++) {
            Xpp3Dom endmemberElement = endmemberElements[i];
            Xpp3Dom endmemberName = endmemberElement.getChild(TAG_NAME);
            if (endmemberName == null) {
                throw new ConversionException("Missing 'endmembers/endmember/name' element.");
            }
            Xpp3Dom radiationsElement = endmemberElement.getChild(TAG_RADIATIONS);
            if (radiationsElement == null) {
                throw new ConversionException("Missing 'endmembers/endmember/radiations' element.");
            }
            double[] radiations = StringUtils.toDoubleArray(radiationsElement.getValue(), ",");
            if (radiations.length != wavelengths.length) {
                throw new ConversionException("'Endmember number of wavelengths does not match number of radiations.");
            }
            endmembers[i] = new Endmember(endmemberName.getValue(), wavelengths, radiations);
        }
        return endmembers;
    }
}