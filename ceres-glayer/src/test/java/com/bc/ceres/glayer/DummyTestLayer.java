package com.bc.ceres.glayer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class DummyTestLayer extends Layer {

    public DummyTestLayer(String name) {
        super(LayerType.getLayerType(Type.class.getName()), name);
    }
}
