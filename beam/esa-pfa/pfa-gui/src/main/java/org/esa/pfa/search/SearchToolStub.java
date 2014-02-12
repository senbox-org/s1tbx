/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.search;

import org.esa.pfa.activelearning.ActiveLearning;
import org.esa.pfa.db.DatasetDescriptor;
import org.esa.pfa.db.PatchQuery;
import org.esa.pfa.fe.op.Patch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 * Stub for PFA Search Tool on the server
 */
public class SearchToolStub {

    private PatchQuery db = null;
    private ActiveLearning al = null;

    private static final java.net.URL dummyURL = SearchToolStub.class.getClassLoader().getResource("images/sigma0_ql.png");
    private static File dummyFile = new File(dummyURL.getPath());

    public SearchToolStub() {
        try {
            db = new PatchQuery(new File("c:\\temp"));
            al = new ActiveLearning();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DatasetDescriptor getDsDescriptor() {
        return db.getDsDescriptor();
    }

    public Dimension getPatchSize(final String featureExtractor) {
        return new Dimension(200, 200);
    }

    public void setQueryImages(final Patch[] queryImages) throws Exception {
        al.setQueryPatches(queryImages);

        Patch[] archivePatches = db.query("product:ENVI*", 50);
        al.setRandomPatches(archivePatches);
    }

    public Patch[] getImagesToLabel(final int numImages) throws Exception {
        Patch[] patchesToLabel = al.getMostAmbiguousPatches(numImages);
        retrievePatchImages(patchesToLabel);

        return patchesToLabel;
    }

    private void retrievePatchImages(final Patch[] patches) {
        for(Patch patch : patches) {
            if(patch.getImage()== null) {
                try {
                    URL imageURL = db.retrievePatchImage(patch);
                    //todo download image
                    File imageFile = new File(imageURL.getPath());
                    patch.setImage(loadImageFile(imageFile));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void trainModel(Patch[] labeledImages) throws Exception {
        al.train(labeledImages);
    }

    public void retrieveImages(final Patch[] rel, final Patch[] irrel) {

    }

    public Patch[] getRetrievedImages(final int numImages) {
        return createDummyImageList(numImages);
    }

    private static Patch[] createDummyImageList(final int size) {
        final Patch[] imageList = new Patch[size];
        for(int i=0; i < imageList.length; ++i) {
            imageList[i] = new Patch(0,0, null, null);
            imageList[i].setImage(loadImageFile(dummyFile));
        }
        return imageList;
    }

    private static BufferedImage loadImageFile(final File file) {
        BufferedImage bufferedImage = null;
        if (file.canRead()) {
            try {
                final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                try {
                    bufferedImage = ImageIO.read(fis);
                } finally {
                    fis.close();
                }
            } catch(Exception e) {
                //
            }
        }
        return bufferedImage;
    }
}
