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

package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.PageComponentContext;
import org.esa.beam.framework.ui.application.PageComponentPane;

public class DefaultPageComponentContext implements PageComponentContext {
    private final PageComponentPane pane;

    private final ApplicationPage page;

    public DefaultPageComponentContext(ApplicationPage page, PageComponentPane pane) {
        Assert.notNull(page, "page");
        this.page = page;
        this.pane = pane;
    }

    @Override
    public ApplicationPage getPage() {
        return page;
    }

    @Override
    public PageComponentPane getPane() {
        return pane;
    }
}