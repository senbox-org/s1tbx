package org.esa.snap.gpf.ui;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * The abstract base class for all operator user interfaces intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 * CreateOpTab() must be implemented in order to create the operator user interface component
 * User: lveci
 * Date: Feb 12, 2008
 */
public abstract class BaseOperatorUI implements OperatorUI {

    protected PropertyContainer valueContainer = null;
    protected Map<String, Object> paramMap = null;
    protected Product[] sourceProducts = null;
    protected String operatorName = "";

    public abstract JComponent CreateOpTab(final String operatorName,
                                           final Map<String, Object> parameterMap, final AppContext appContext);

    public abstract void initParameters();

    public abstract UIValidation validateParameters();

    public abstract void updateParameters();

    public String getOperatorName() {
        return operatorName;
    }

    protected void initializeOperatorUI(final String operatorName, final Map<String, Object> parameterMap) {
        this.operatorName = operatorName;

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }

        paramMap = parameterMap;
        final ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        valueContainer = PropertyContainer.createMapBacked(paramMap, operatorSpi.getOperatorClass(), parameterDescriptorFactory);

        if (paramMap.isEmpty()) {
            try {
                valueContainer.setDefaultValues();
            } catch (IllegalStateException e) {
                // todo - handle exception here
                e.printStackTrace();
            }
        }
    }

    public void setSourceProducts(final Product[] products) {
        if (sourceProducts == null || !Arrays.equals(sourceProducts, products)) {
            sourceProducts = products;
            if (paramMap != null) {
                initParameters();
            }
        }
    }

    public boolean hasSourceProducts() {
        return sourceProducts != null;
    }

    public void convertToDOM(final XppDomElement parentElement) {

        if (valueContainer == null) {
            setParamsToConfiguration(parentElement.getXppDom());
            return;
        }

        final Property[] properties = valueContainer.getProperties();
        for (Property p : properties) {
            final PropertyDescriptor descriptor = p.getDescriptor();
            final DomConverter domConverter = descriptor.getDomConverter();
            if (domConverter != null) {
                try {
                    final DomElement childElement = parentElement.createChild(getElementName(p));
                    domConverter.convertValueToDom(p.getValue(), childElement);
                } catch (ConversionException e) {
                    e.printStackTrace();
                }
            } else {

                final String itemAlias = descriptor.getItemAlias();
                if (descriptor.getType().isArray() && itemAlias != null && !itemAlias.isEmpty()) {
                    final DomElement childElement = descriptor.getItemsInlined() ? parentElement : parentElement.createChild(getElementName(p));
                    final Object array = p.getValue();
                    final Converter itemConverter = getItemConverter(descriptor);
                    if (array != null && itemConverter != null) {
                        final int arrayLength = Array.getLength(array);
                        for (int i = 0; i < arrayLength; i++) {
                            final Object component = Array.get(array, i);
                            final DomElement itemElement = childElement.createChild(itemAlias);

                            final String text = itemConverter.format(component);
                            if (text != null && !text.isEmpty()) {
                                itemElement.setValue(text);
                            }
                        }
                    }
                } else {
                    final DomElement childElement = parentElement.createChild(getElementName(p));
                    final Object childValue = p.getValue();
                    final Converter converter = descriptor.getConverter();

                    final String text = converter.format(childValue);
                    if (text != null && !text.isEmpty()) {
                        childElement.setValue(text);
                    }
                }
            }
        }
    }

    protected String[] getBandNames() {
        final ArrayList<String> bandNames = new ArrayList<String>(5);
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (sourceProducts.length > 1) {
                    for (String name : prod.getBandNames()) {
                        bandNames.add(name + "::" + prod.getName());
                    }
                } else {
                    bandNames.addAll(Arrays.asList(prod.getBandNames()));
                }
            }
        }
        return bandNames.toArray(new String[bandNames.size()]);
    }

    protected boolean isComplexSrcProduct() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            for (Band band : sourceProducts[0].getBands()) {
                final String unit = band.getUnit();
                if (unit != null && (unit.contains("real") || unit.contains("imaginary"))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String[] getGeometries() {
        final ArrayList<String> geometryNames = new ArrayList<String>(5);
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (sourceProducts.length > 1) {
                    for (String name : prod.getMaskGroup().getNodeNames()) {
                        geometryNames.add(name + "::" + prod.getName());
                    }
                } else {
                    geometryNames.addAll(Arrays.asList(prod.getMaskGroup().getNodeNames()));
                }
            }
        }
        return geometryNames.toArray(new String[geometryNames.size()]);
    }

    private void setParamsToConfiguration(final XppDom config) {
        if (paramMap == null) return;
        final Set<String> keys = paramMap.keySet();                     // The set of keys in the map.
        for (String key : keys) {
            final Object value = paramMap.get(key);             // Get the value for that key.
            if (value == null) continue;

            XppDom xml = config.getChild(key);
            if (xml == null) {
                xml = new XppDom(key);
                config.addChild(xml);
            }

            xml.setValue(value.toString());
        }
    }

    private static Converter getItemConverter(final PropertyDescriptor descriptor) {
        final Class<?> itemType = descriptor.getType().getComponentType();
        Converter itemConverter = descriptor.getConverter();
        if (itemConverter == null) {
            itemConverter = ConverterRegistry.getInstance().getConverter(itemType);
        }
        return itemConverter;
    }

    private static String getElementName(final Property p) {
        final String alias = p.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return p.getDescriptor().getName();
    }
}