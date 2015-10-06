package org.esa.snap.core.gpf.common.support;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.snap.core.gpf.common.BandMathsOp;

import java.util.ArrayList;
import java.util.List;

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

        for(BandMathsOp.BandDescriptor bandDescriptor : bandDescriptors) {
            DomElement targetBand = parentElement.createChild("targetBand");

            final DomElement name = targetBand.createChild("name");
            name.setValue(bandDescriptor.name);


            DomElement type = targetBand.createChild("type");
            type.setValue(bandDescriptor.type);

            DomElement expression = targetBand.createChild("expression");
            expression.setValue(bandDescriptor.expression);

            DomElement description = targetBand.createChild("description");
            description.setValue(bandDescriptor.description);

            DomElement unit = targetBand.createChild("unit");
            unit.setValue(bandDescriptor.unit);

            if(bandDescriptor.validExpression != null) {
                DomElement validExpression = targetBand.createChild("validExpression");
                validExpression.setValue(bandDescriptor.validExpression);
            }

            if(bandDescriptor.noDataValue != null) {
                DomElement noDataValue = targetBand.createChild("noDataValue");
                noDataValue.setValue(String.valueOf(bandDescriptor.noDataValue));
            }

            if(bandDescriptor.spectralBandIndex != null) {
                DomElement spectralBandIndex = targetBand.createChild("spectralBandIndex");
                spectralBandIndex.setValue(String.valueOf(bandDescriptor.spectralBandIndex));
            }

            if(bandDescriptor.spectralWavelength != null) {
                DomElement spectralWavelength = targetBand.createChild("spectralWavelength");
                spectralWavelength.setValue(String.valueOf(bandDescriptor.spectralWavelength));
            }

            if(bandDescriptor.spectralBandwidth != null) {
                DomElement spectralBandwidth = targetBand.createChild("spectralBandwidth");
                spectralBandwidth.setValue(String.valueOf(bandDescriptor.spectralBandwidth));
            }

            if(bandDescriptor.scalingOffset != null) {
                DomElement scalingOffset = targetBand.createChild("scalingOffset");
                scalingOffset.setValue(String.valueOf(bandDescriptor.scalingOffset));
            }

            if(bandDescriptor.scalingFactor != null) {
                DomElement scalingFactor = targetBand.createChild("scalingFactor");
                scalingFactor.setValue(String.valueOf(bandDescriptor.scalingFactor));
            }
        }
    }
}
