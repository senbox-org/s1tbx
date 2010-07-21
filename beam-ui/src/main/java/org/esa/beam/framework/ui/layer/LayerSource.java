/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.layer;

/**
 * A layer source can add one or more layers
 * to an already existing root layer.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public interface LayerSource {

    /**
     * Check if this layer source is applicable to the current context.
     *
     * @param pageContext The current context.
     * @return true, if this layer source is applicable.
     */
    boolean isApplicable(LayerSourcePageContext pageContext);

    /**
     * @return true, if this layer source has assistant pages.
     */
    boolean hasFirstPage();

    /**
     * Returns the first page (of possible many) of the assistant
     * that belongs to this layer source. The given context can be
     * interrogated to decide which page to return.
     *
     * @param pageContext The current context.
     * @return the first assistant page.
     */
    AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext);

    /**
     * Checks whether this layer source can perform its finishing
     * method without further information.
     *
     * @param pageContext The current context.
     * @return true, if finish can be called.
     */
    boolean canFinish(LayerSourcePageContext pageContext);

    /**
     * Adds one or more layers to the given context
     *
     * @param pageContext The current context.
     * @return true, if the method completed successfully
     */
    boolean performFinish(LayerSourcePageContext pageContext);

    /**
     * Aborts the operation of this layer source.
     * This method is responsible for freeing all resources acquired by
     * the layer source.
     *
     * @param pageContext The current context.
     */
    void cancel(LayerSourcePageContext pageContext);
}
