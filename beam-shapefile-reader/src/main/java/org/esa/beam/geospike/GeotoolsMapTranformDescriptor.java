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

import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.param.Parameter;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class GeotoolsMapTranformDescriptor implements MapTransformDescriptor {

    private final String name;
    
    public GeotoolsMapTranformDescriptor(String name) {
        this.name = name;
    }
    @Override
    public MapTransform createTransform(double[] parameterValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMapUnit() {
        return "unknown";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double[] getParameterDefaultValues() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MapTransformUI getTransformUI(MapTransform transform) {
        return null;
    }

    @Override
    public String getTypeID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasTransformUI() {
        return false;
    }

    @Override
    public void registerProjections() {
        // no registration here
    }
}
