package org.esa.snap.gpf.operators.standard;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 24/06/2015.
 */
public class BandDescriptorDomConverter implements DomConverter {

    public Class<?> getValueType() {
        return BandMathsOp.BandDescriptor[].class;
    }

    public BandMathsOp.BandDescriptor[] convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {

        final List<BandMathsOp.BandDescriptor> targetBandList = new ArrayList<>();

        final DomElement[] targetBands = parentElement.getChildren();
        for(DomElement targetBand : targetBands) {
            final BandMathsOp.BandDescriptor desc = new BandMathsOp.BandDescriptor();
            targetBandList.add(desc);

            DomElement name = targetBand.getChild("name");
            if (name != null) {
                desc.name = name.getValue();
            }

            DomElement type = targetBand.getChild("type");
            if (type != null) {
                desc.type = type.getValue();
            }

            DomElement expression = targetBand.getChild("expression");
            if (expression != null) {
                desc.expression = expression.getValue();
            }

            DomElement description = targetBand.getChild("description");
            if (description != null) {
                desc.description = description.getValue();
            }

            DomElement unit = targetBand.getChild("unit");
            if (unit != null) {
                desc.unit = unit.getValue();
            }

            DomElement validExpression = targetBand.getChild("validExpression");
            if (validExpression != null) {
                desc.validExpression = validExpression.getValue();
            }

            DomElement noDataValue = targetBand.getChild("noDataValue");
            if (noDataValue != null) {
                desc.noDataValue = Double.parseDouble(noDataValue.getValue());
            }

            DomElement spectralBandIndex = targetBand.getChild("spectralBandIndex");
            if (spectralBandIndex != null) {
                if(spectralBandIndex.getValue().equals("null"))
                    desc.spectralBandIndex = null;
                else
                    desc.spectralBandIndex = Integer.parseInt(spectralBandIndex.getValue());
            }

            DomElement spectralWavelength = targetBand.getChild("spectralWavelength");
            if (spectralWavelength != null) {
                if(spectralWavelength.getValue().equals("null"))
                    desc.spectralWavelength = null;
                else
                    desc.spectralWavelength = Float.parseFloat(spectralWavelength.getValue());
            }

            DomElement spectralBandwidth = targetBand.getChild("spectralBandwidth");
            if (spectralBandwidth != null) {
                if(spectralBandwidth.getValue().equals("null"))
                    desc.spectralBandwidth = null;
                else
                    desc.spectralBandwidth = Float.parseFloat(spectralBandwidth.getValue());
            }

            DomElement scalingOffset = targetBand.getChild("scalingOffset");
            if (scalingOffset != null) {
                if(scalingOffset.getValue().equals("null"))
                    desc.scalingOffset = null;
                else
                    desc.scalingOffset = Double.parseDouble(scalingOffset.getValue());
            }

            DomElement scalingFactor = targetBand.getChild("scalingFactor");
            if (scalingFactor != null) {
                if(scalingFactor.getValue().equals("null"))
                    desc.scalingFactor = null;
                else
                    desc.scalingFactor = Double.parseDouble(scalingFactor.getValue());
            }
        }

        return targetBandList.toArray(new BandMathsOp.BandDescriptor[targetBandList.size()]);
    }


    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        final BandMathsOp.BandDescriptor[] bandDescriptors = (BandMathsOp.BandDescriptor[])value;

        DomElement targetBands = parentElement;

        for(BandMathsOp.BandDescriptor bandDescriptor : bandDescriptors) {
            DomElement targetBand = new XppDomElement("targetBand");
            targetBands.addChild(targetBand);

            DomElement name = new XppDomElement("name");
            name.setValue(bandDescriptor.name);
            targetBand.addChild(name);

            DomElement type = new XppDomElement("type");
            type.setValue(bandDescriptor.type);
            targetBand.addChild(type);

            DomElement expression = new XppDomElement("expression");
            expression.setValue(bandDescriptor.expression);
            targetBand.addChild(expression);

            DomElement description = new XppDomElement("description");
            description.setValue(bandDescriptor.description);
            targetBand.addChild(description);

            DomElement unit = new XppDomElement("unit");
            unit.setValue(bandDescriptor.unit);
            targetBand.addChild(unit);

            if(bandDescriptor.validExpression != null) {
                DomElement validExpression = new XppDomElement("validExpression");
                validExpression.setValue(bandDescriptor.validExpression);
                targetBand.addChild(validExpression);
            }

            if(bandDescriptor.noDataValue != null) {
                DomElement noDataValue = new XppDomElement("noDataValue");
                noDataValue.setValue(String.valueOf(bandDescriptor.noDataValue));
                targetBand.addChild(noDataValue);
            }

            if(bandDescriptor.spectralBandIndex != null) {
                DomElement spectralBandIndex = new XppDomElement("spectralBandIndex");
                spectralBandIndex.setValue(String.valueOf(bandDescriptor.spectralBandIndex));
                targetBand.addChild(spectralBandIndex);
            }

            if(bandDescriptor.spectralWavelength != null) {
                DomElement spectralWavelength = new XppDomElement("spectralWavelength");
                spectralWavelength.setValue(String.valueOf(bandDescriptor.spectralWavelength));
                targetBand.addChild(spectralWavelength);
            }

            if(bandDescriptor.spectralBandwidth != null) {
                DomElement spectralBandwidth = new XppDomElement("spectralBandwidth");
                spectralBandwidth.setValue(String.valueOf(bandDescriptor.spectralBandwidth));
                targetBand.addChild(spectralBandwidth);
            }

            if(bandDescriptor.scalingOffset != null) {
                DomElement scalingOffset = new XppDomElement("scalingOffset");
                scalingOffset.setValue(String.valueOf(bandDescriptor.scalingOffset));
                targetBand.addChild(scalingOffset);
            }

            if(bandDescriptor.scalingFactor != null) {
                DomElement scalingFactor = new XppDomElement("scalingFactor");
                scalingFactor.setValue(String.valueOf(bandDescriptor.scalingFactor));
                targetBand.addChild(scalingFactor);
            }
        }
    }
}
