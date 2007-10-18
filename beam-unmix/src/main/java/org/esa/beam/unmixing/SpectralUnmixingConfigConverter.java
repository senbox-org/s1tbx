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

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ParameterConverter;
import org.esa.beam.util.StringUtils;

public class SpectralUnmixingConfigConverter implements ParameterConverter {

    public void getConfigurationTemplate(Xpp3Dom configuration) {
        final Xpp3Dom endmembers = new Xpp3Dom("endmembers");

        final Xpp3Dom wavelengths = new Xpp3Dom("wavelengths");
        wavelengths.setValue("Comma separated list of wavelength");
        endmembers.addChild(wavelengths);

        final Xpp3Dom endmember = new Xpp3Dom("endmember");

        final Xpp3Dom name = new Xpp3Dom("name");
        name.setValue("Name of the endmember");
        endmember.addChild(name);

        final Xpp3Dom radiations = new Xpp3Dom("radiations");
        radiations.setValue("Comma separated list of radiations, size equal to #wavelengths)");
        endmember.addChild(radiations);

        endmembers.addChild(endmember);
        configuration.addChild(endmembers);
    }

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        configureSourceBands((SpectralUnmixingOp) operator, configuration);
        configureEndmembers((SpectralUnmixingOp) operator, configuration);
    }

    private void configureEndmembers(SpectralUnmixingOp operator, Xpp3Dom configuration) throws OperatorException {
        Xpp3Dom endmembersElement = configuration.getChild("endmembers");
        Xpp3Dom wavelengthsElement = endmembersElement.getChild("wavelengths");
        if (wavelengthsElement == null) {
            throw new OperatorException("Missing 'endmembers/wavelengths' element.");
        }
        double[] wavelengths = StringUtils.toDoubleArray(wavelengthsElement.getValue(), ",");

        Xpp3Dom[] endmemberElements = endmembersElement.getChildren("endmember");
        if (endmemberElements.length == 0) {
            throw new OperatorException("Missing 'endmembers/endmember' elements.");
        }
        operator.endmembers = new Endmember[endmemberElements.length];
        for (int i = 0; i < endmemberElements.length; i++) {
            Xpp3Dom endmemberElement = endmemberElements[i];
            Xpp3Dom endmemberName = endmemberElement.getChild("name");
            if (endmemberName == null) {
                throw new OperatorException("Missing 'endmembers/endmember/name' element.");
            }
            Xpp3Dom radiationsElement = endmemberElement.getChild("radiations");
            if (radiationsElement == null) {
                throw new OperatorException("Missing 'endmembers/endmember/radiations' element.");
            }
            double[] radiations = StringUtils.toDoubleArray(radiationsElement.getValue(), ",");
            if (radiations.length != wavelengths.length) {
                throw new OperatorException("'Endmember number of wavelengths does not match number of radiations.");
            }
            operator.endmembers[i] = new Endmember(endmemberName.getValue(), wavelengths, radiations);
        }
    }

    private void configureSourceBands(SpectralUnmixingOp operator, Xpp3Dom configuration) {
        Xpp3Dom sourceBandsElement = configuration.getChild("sourceBands");
        Xpp3Dom[] bandElements = sourceBandsElement.getChildren("band");
        operator.sourceBandNames = new String[bandElements.length];
        for (int i = 0; i < bandElements.length; i++) {
            operator.sourceBandNames[i] = bandElements[i].getValue();
        }
    }

}