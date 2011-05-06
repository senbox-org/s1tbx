/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * Support for operator parameters input/output.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class OperatorParameterSupport {

    private static final ParameterUpdater DEFAULT = new ParameterUpdater() {
        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) {
        }
    };

    private final Class<? extends Operator> opType;
    private final ParameterDescriptorFactory descriptorFactory;
    private final Map<String, Object> parameterMap;
    private final PropertySet propertySet;
    private final ParameterUpdater parameterUpdater;

    /**
     * Creates a parameter support for the given operator type.
     *
     * @param opType The operator type.
     */
    public OperatorParameterSupport(Class<? extends Operator> opType) {
        this(opType, null, null, null);
    }

    /**
     * Creates a parameter support, for the given operator type.
     * <p/>
     * If a property set and a parameter map are given the client as to keep them in sync.
     * The {@code parameterUpdater} will be called before each save  and after each load request to
     * enable custom updating.
     *
     * @param opType  The operator type (mandatory).
     * @param propertySet The property set (can be null). If supplied a parameter map is required as well.
     * @param parameterMap the parameter map (can be null)
     * @param parameterUpdater The parameter updater (can be null)
     */
    public OperatorParameterSupport(Class<? extends Operator> opType,
                                     PropertySet propertySet,
                                     Map<String, Object> parameterMap,
                                     ParameterUpdater parameterUpdater) {
        Assert.notNull(opType, "opType");
        Assert.argument(parameterMap != null || propertySet == null, "parameterMap != null || propertySet == null");

        this.opType = opType;
        this.descriptorFactory = new ParameterDescriptorFactory();

        if (parameterMap == null) {
            parameterMap = new HashMap<String, Object>();
        }
        this.parameterMap = parameterMap;

        if (propertySet == null) {
            propertySet = PropertyContainer.createMapBacked(parameterMap, opType, descriptorFactory);
            propertySet.setDefaultValues();
        }
        this.propertySet = propertySet;

        if (parameterUpdater == null) {
            parameterUpdater = DEFAULT;
        }
        this.parameterUpdater = parameterUpdater;
    }

    public PropertySet getPopertySet() {
        return propertySet;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void fromDomElement(DomElement parametersElement) throws ValidationException, ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        domConverter.convertDomToValue(parametersElement, propertySet);
        parameterUpdater.handleParameterLoadRequest(parameterMap);
    }

    public DomElement toDomElement() throws ValidationException, ConversionException {
        parameterUpdater.handleParameterSaveRequest(parameterMap);
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        DefaultDomElement parametersElement = new DefaultDomElement("parameters");
        domConverter.convertValueToDom(propertySet, parametersElement);
        return parametersElement;
    }
}
