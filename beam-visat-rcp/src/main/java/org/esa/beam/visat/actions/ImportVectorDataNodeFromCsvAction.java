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

package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.geometry.VectorDataNodeReader2;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ImportVectorDataNodeFromCsvAction extends AbstractImportVectorDataNodeAction {

    private VectorDataNodeImporter importer;


    @Override
    public void actionPerformed(final CommandEvent event) {
        final BeamFileFilter filter = new BeamFileFilter("CSV",
                                                         new String[]{".txt", ".dat", ".csv"},
                                                         "Plain text");
        importer = new VectorDataNodeImporter(getHelpId(), filter, new MyVectorDataNodeReader(), "Import CSV file", "csv.io.dir");
        importer.importGeometry(VisatApp.getApp());
        VisatApp.getApp().updateState();
    }


    @Override
    public void updateState(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null);
    }

    @Override
    protected String getDialogTitle() {
        return importer.getDialogTitle();
    }

    private class MyVectorDataNodeReader implements VectorDataNodeImporter.VectorDataNodeReader {

        @Override
        public VectorDataNode readVectorDataNode(VisatApp visatApp, File file, Product product, String helpId, ProgressMonitor pm) throws IOException {
            final CoordinateReferenceSystem modelCrs = product.getGeoCoding() != null ? ImageManager.getModelCrs(product.getGeoCoding()) :
                    ImageManager.DEFAULT_IMAGE_CRS;
            return VectorDataNodeReader2.read(file.getName(), new FileReader(file), product, crsProvider, placemarkDescriptorProvider, modelCrs, pm);
        }
    }
}
