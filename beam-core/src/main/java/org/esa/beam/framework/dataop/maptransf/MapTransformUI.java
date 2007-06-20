/*
 * $Id: MapTransformUI.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.maptransf;

import java.awt.Component;

public interface MapTransformUI {

    MapTransform createTransform();

    void resetToDefaults();

    boolean verifyUserInput();

    Component getUIComponent();
}
