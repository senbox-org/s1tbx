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
package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceController;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ShapefileController implements LayerSourceController {

    @Override
    public boolean isApplicable(AppAssistantPageContext pageContext) {
        return true;
    }
    
    @Override
    public boolean hasFirstPage() {
        return true;
    }

    @Override
    public AbstractAppAssistantPage getFirstPage(AppAssistantPageContext pageContext) {
        return new ShapefileAssistantPage();
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean finish(AppAssistantPageContext pageContext) {
        return pageContext.getCurrentPage().performFinish(pageContext);
    }
}
