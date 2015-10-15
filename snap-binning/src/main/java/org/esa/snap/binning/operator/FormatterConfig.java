/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.snap.binning.operator;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.binding.ConversionException;
import org.esa.snap.binning.ProductCustomizer;
import org.esa.snap.binning.ProductCustomizerConfig;
import org.esa.snap.binning.ProductCustomizerDescriptor;
import org.esa.snap.binning.TypedDescriptorsRegistry;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.ParameterBlockConverter;

/**
 * The configuration for the {@link Formatter}.
 *
 * @author Norman Fomferra
 */
public class FormatterConfig {

    @Parameter(valueSet = {"Product", "RGB", "Grey"}, defaultValue = "Product")
    private String outputType;
    @Parameter
    private String outputFile;
    @Parameter(defaultValue = "BEAM-DIMAP")
    private String outputFormat;
    @Parameter(alias = "outputBands", itemAlias = "band", description = "Configures the target bands. Not needed " +
            "if output type 'Product' is chosen.")
    private BinningOp.BandConfiguration[] bandConfigurations;
    @Parameter(alias = "productCustomizer", domConverter = ProductCustomizerConfigDomConverter.class)
    private ProductCustomizerConfig productCustomizerConfig;

    private transient ProductCustomizer productCustomizer;

    public FormatterConfig() {
        // used by DOM converter
    }

    public FormatterConfig(String outputType,
                           String outputFile,
                           String outputFormat,
                           BinningOp.BandConfiguration[] bandConfigurations) {
        this.outputType = outputType;
        this.outputFile = outputFile;
        this.outputFormat = outputFormat;
        this.bandConfigurations = bandConfigurations;
    }

    /**
     * Creates a new formatter configuration object.
     *
     * @param xml The configuration as an XML string.
     * @return The new formatter configuration object.
     * @throws com.bc.ceres.binding.BindingException
     *          If the XML cannot be converted to a new formatter configuration object
     */
    public static FormatterConfig fromXml(String xml) throws BindingException {
        return new ParameterBlockConverter().convertXmlToObject(xml, new FormatterConfig());
    }

    public String toXml() {
        try {
            return new ParameterBlockConverter().convertObjectToXml(this);
        } catch (ConversionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getOutputType() {
        if (outputType == null) {
            throw new IllegalArgumentException("No output type given");
        }
        if (!outputType.equalsIgnoreCase("Product")
                && !outputType.equalsIgnoreCase("RGB")
                && !outputType.equalsIgnoreCase("Grey")) {
            throw new IllegalArgumentException("Unknown output type: " + outputType);
        }
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public BinningOp.BandConfiguration[] getBandConfigurations() {
        return bandConfigurations.clone();
    }

    public void setBandConfigurations(BinningOp.BandConfiguration... bandConfigurations) {
        this.bandConfigurations = bandConfigurations;
    }

    public ProductCustomizerConfig getProductCustomizerConfig() {
        return productCustomizerConfig;
    }

    public void setProductCustomizerConfig(ProductCustomizerConfig productCustomizerConfig) {
        this.productCustomizerConfig = productCustomizerConfig;
    }

    public ProductCustomizer getProductCustomizer() {
        if (productCustomizer == null && productCustomizerConfig != null) {
            TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
            ProductCustomizerDescriptor descriptor = registry.getDescriptor(ProductCustomizerDescriptor.class, productCustomizerConfig.getName());
            if (descriptor != null) {
                productCustomizer = descriptor.createProductCustomizer(productCustomizerConfig);
            } else {
                throw new IllegalArgumentException("Unknown cell processor type: " + productCustomizerConfig.getName());
            }
        }
        return productCustomizer;
    }
}
