package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public interface LayerSourceDescriptor {

    String getId();

    String getName();

    String getDescription();

    LayerSourceController createController();

    LayerType getLayerType();
}
