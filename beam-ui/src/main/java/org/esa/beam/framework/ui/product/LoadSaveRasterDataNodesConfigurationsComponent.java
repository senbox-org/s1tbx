package org.esa.beam.framework.ui.product;

import java.awt.Window;

public interface LoadSaveRasterDataNodesConfigurationsComponent {

    void setReadRasterDataNodeNames(String[] readRasterDataNodeNames);

    String[] getRasterDataNodeNamesToWrite();

    Window getParent();

}
