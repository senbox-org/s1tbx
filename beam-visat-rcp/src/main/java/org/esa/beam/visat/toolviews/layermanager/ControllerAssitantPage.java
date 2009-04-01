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
package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;

import java.awt.Component;


public class ControllerAssitantPage extends AbstractAppAssistantPage {

    private final LayerSource layerSource;
    private final AbstractAppAssistantPage appAssistantPage;

    public ControllerAssitantPage(AbstractAppAssistantPage appAssistantPage, LayerSource layerSource) {
        super(appAssistantPage.getPageTitle());
        this.appAssistantPage = appAssistantPage;
        this.layerSource = layerSource;
    }

    public LayerSource getLayerSourceController() {
        return layerSource;
    }

    @Override
    public boolean performFinish(AppAssistantPageContext pageContext) {
        return layerSource.finish(pageContext);
    }

    @Override
    public Component createLayerPageComponent(AppAssistantPageContext context) {
        return appAssistantPage.createLayerPageComponent(context);
    }

    @Override
    public boolean canFinish() {
        return appAssistantPage.canFinish();
    }

    @Override
    public boolean canHelp() {
        return appAssistantPage.canHelp();
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        return new ControllerAssitantPage(appAssistantPage.getNextPage(pageContext), getLayerSourceController());
    }

    @Override
    public Component getPageComponent() {
        return appAssistantPage.getPageComponent();
    }

    @Override
    public String getPageTitle() {
        return appAssistantPage.getPageTitle();
    }

    @Override
    public boolean hasNextPage() {
        return appAssistantPage.hasNextPage();
    }

    @Override
    public void performCancel() {
        appAssistantPage.performCancel();
    }

    @Override
    public void performHelp() {
        appAssistantPage.performHelp();
    }

    @Override
    public void setPageTitle(String pageTitle) {
        appAssistantPage.setPageTitle(pageTitle);
    }

    @Override
    public boolean validatePage() {
        return appAssistantPage.validatePage();
    }
}
