/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.gpf.operators.standard;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.StringReader;

/**
 * A converter for BandDecriptor
 *
 * @author Luis Veci
 */
public class BandDescriptorConverter implements Converter<BandMathsOp.BandDescriptor> {


    private static final String EXCEPTION_FORMAT_PATTERN = "Invalid BandDescriptor '%s' should be in form of "+"" +
            "<targetBand>" +
            "  <name>string</name>" +
            "  <type>string</type>" +
            "  <expression>string</expression>" +
            "</targetBand>";

    @Override
    public Class<BandMathsOp.BandDescriptor> getValueType() {
        return BandMathsOp.BandDescriptor.class;
    }

    @Override
    public BandMathsOp.BandDescriptor parse(String text) throws ConversionException {
        if (text != null) {
            text = text.trim();
        }
        if (text == null || text.isEmpty()) {
            throw new ConversionException(String.format(EXCEPTION_FORMAT_PATTERN, text));
        }

        BandMathsOp.BandDescriptor desc = new BandMathsOp.BandDescriptor();
        try {
            final Document doc = new SAXBuilder().build(new StringReader(text));
            final Element root = doc.getRootElement();

            Element name = root.getChild("name");
            if(name != null) {
                desc.name = name.getValue();
            }

            Element type = root.getChild("type");
            if(type != null) {
                desc.type = type.getValue();
            }

            Element expression = root.getChild("expression");
            if(expression != null) {
                desc.expression = expression.getValue();
            }

            Element description = root.getChild("description");
            if(description != null) {
                desc.description = description.getValue();
            }

            Element unit = root.getChild("unit");
            if(unit != null) {
                desc.unit = unit.getValue();
            }

            Element validExpression = root.getChild("validExpression");
            if(validExpression != null) {
                desc.validExpression = validExpression.getValue();
            }

            Element noDataValue = root.getChild("noDataValue");
            if(noDataValue != null) {
                desc.noDataValue = Double.parseDouble(noDataValue.getValue());
            }

            Element spectralBandIndex = root.getChild("spectralBandIndex");
            if(spectralBandIndex != null) {
                desc.spectralBandIndex = Integer.parseInt(spectralBandIndex.getValue());
            }

            Element spectralWavelength = root.getChild("spectralWavelength");
            if(spectralWavelength != null) {
                desc.spectralWavelength = Float.parseFloat(spectralWavelength.getValue());
            }

            Element spectralBandwidth = root.getChild("spectralBandwidth");
            if(spectralBandwidth != null) {
                desc.spectralBandwidth = Float.parseFloat(spectralBandwidth.getValue());
            }

            Element scalingOffset = root.getChild("scalingOffset");
            if(scalingOffset != null) {
                desc.scalingOffset = Double.parseDouble(scalingOffset.getValue());
            }

            Element scalingFactor = root.getChild("scalingFactor");
            if(scalingFactor != null) {
                desc.scalingFactor = Double.parseDouble(scalingFactor.getValue());
            }

        } catch (Exception e) {
            throw new ConversionException(String.format(EXCEPTION_FORMAT_PATTERN, text));
        }

        return desc;
    }

    @Override
    public String format(final BandMathsOp.BandDescriptor bandDescriptor) {
        if (bandDescriptor == null) {
            return "";
        }

        Element targetBand = new Element("targetBand");

        Element name = new Element("name");
        name.setText(bandDescriptor.name);
        targetBand.addContent(name);

        Element type = new Element("type");
        type.setText(bandDescriptor.type);
        targetBand.addContent(type);

        Element expression = new Element("expression");
        expression.setText(bandDescriptor.expression);
        targetBand.addContent(expression);

        Element description = new Element("description");
        description.setText(bandDescriptor.description);
        targetBand.addContent(description);

        Element unit = new Element("unit");
        unit.setText(bandDescriptor.unit);
        targetBand.addContent(unit);

        Element validExpression = new Element("validExpression");
        validExpression.setText(bandDescriptor.validExpression);
        targetBand.addContent(validExpression);

        Element noDataValue = new Element("noDataValue");
        noDataValue.setText(String.valueOf(bandDescriptor.noDataValue));
        targetBand.addContent(noDataValue);

        Element spectralBandIndex = new Element("spectralBandIndex");
        spectralBandIndex.setText(String.valueOf(bandDescriptor.spectralBandIndex));
        targetBand.addContent(spectralBandIndex);

        Element spectralWavelength = new Element("spectralWavelength");
        spectralWavelength.setText(String.valueOf(bandDescriptor.spectralWavelength));
        targetBand.addContent(spectralWavelength);

        Element spectralBandwidth = new Element("spectralBandwidth");
        spectralBandwidth.setText(String.valueOf(bandDescriptor.spectralBandwidth));
        targetBand.addContent(spectralBandwidth);

        Element scalingOffset = new Element("scalingOffset");
        scalingOffset.setText(String.valueOf(bandDescriptor.scalingOffset));
        targetBand.addContent(scalingOffset);

        Element scalingFactor = new Element("scalingFactor");
        scalingFactor.setText(String.valueOf(bandDescriptor.scalingFactor));
        targetBand.addContent(scalingFactor);

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        return outputter.outputString(targetBand);
    }
}
