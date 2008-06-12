/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.graph;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class Header {

    //Metadata ???
    // TargetProperties ???
    // UI later
    // Help later
    private List<HeaderParameter> parameters;
    private List<HeaderSource> sources;
    private HeaderTarget target;
    
    /**
     * @return the id of the target node
     */
    public HeaderTarget getTarget() {
        return target;
    }
    
    /**
     * @return the sources list
     */
    public List<HeaderSource> getSources() {
        return sources;
    }
    
    /**
     * @return the parameters
     */
    public List<HeaderParameter> getParameters() {
        return parameters;
    }
    
    /**
     * Indirectly used by {@link GraphIO}. DO NOT REMOVE!
     *
     * @return this
     */
    private Object readResolve() {
        init();
        return this;
    }

    private void init() {
        if (sources == null) {
            sources = new ArrayList<HeaderSource>(7);
        }
        if (parameters == null) {
            parameters = new ArrayList<HeaderParameter>(7);
        }
    }
}
