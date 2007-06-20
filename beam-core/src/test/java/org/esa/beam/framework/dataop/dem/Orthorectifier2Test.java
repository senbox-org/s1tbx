/*
 * $Id: Orthorectifier2Test.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

public class Orthorectifier2Test extends OrthorectifierTest {

    Orthorectifier createOrthorectifier() {
        return new Orthorectifier2(SCENE_WIDTH,
                                   SCENE_HEIGHT,
                                   new PointingMock(new GeoCodingMock()),
                                   null,
                                   MAX_ITERATION_COUNT);
    }
}
