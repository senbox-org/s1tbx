/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.geospike;

import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.param.Parameter;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class GeotoolsMapTranformDescriptorRegistrator implements MapTransformDescriptor {

    @Override
    public MapTransform createTransform(double[] parameterValues) {
        return null;
    }

    @Override
    public String getMapUnit() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public double[] getParameterDefaultValues() {
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        return null;
    }

    @Override
    public MapTransformUI getTransformUI(MapTransform transform) {
        return null;
    }

    @Override
    public String getTypeID() {
        return null;
    }

    @Override
    public boolean hasTransformUI() {
        return false;
    }

    @Override
    public void registerProjections() {
        // TODO Auto-generated method stub
        // register all geotools mapprojections as GeotoolsMapTranformDescriptor
        
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
        final MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final Collection<OperationMethod> methods = mtFactory.getAvailableMethods(Projection.class);
//        final Map<String,?> dummyName = Collections.singletonMap("name", "Test");
        for (final OperationMethod method : methods) {
            final String classification = method.getName().getCode();
            ParameterDescriptorGroup parameters = method.getParameters();
            ParameterValueGroup param;
            try {
                param = mtFactory.getDefaultParameters(classification);
                MapTransformDescriptor mapTranformDescriptor = new GeotoolsMapTranformDescriptor(classification);
                MapTransform geoToolsMapTransform = new GeoToolsMapTransform(param);
                MapProjection mapProjection = new MapProjection(classification, geoToolsMapTransform);
                MapProjectionRegistry.registerProjection(mapProjection);
            } catch (NoSuchIdentifierException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    }
}
