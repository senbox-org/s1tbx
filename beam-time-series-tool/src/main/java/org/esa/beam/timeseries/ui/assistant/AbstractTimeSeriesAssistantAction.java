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

package org.esa.beam.timeseries.ui.assistant;

import org.esa.beam.framework.ui.assistant.AssistantPane;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

abstract class AbstractTimeSeriesAssistantAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        super.actionPerformed(event);
        final TimeSeriesAssistantModel assistantModel = createModel();
        final AssistantPane assistant = new AssistantPane(getAppContext().getApplicationWindow(), "New Time Series");
        assistant.show(new TimeSeriesAssistantPage_SourceProducts(assistantModel));
    }

    protected abstract TimeSeriesAssistantModel createModel();

}