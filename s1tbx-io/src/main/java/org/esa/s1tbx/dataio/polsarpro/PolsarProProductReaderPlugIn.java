/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dataio.polsarpro;

import org.esa.snap.dataio.envi.EnviProductReaderPlugIn;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.RGBImageProfile;
import org.esa.snap.framework.datamodel.RGBImageProfileManager;

import java.io.File;
import java.util.Locale;

public class PolsarProProductReaderPlugIn extends EnviProductReaderPlugIn {

    static{
        registerRGBProfiles();
    }

    public static final String FORMAT_NAME = "PolSARPro";

    @Override
    public ProductReader createReaderInstance() {
        return new PolsarProProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String getDescription(Locale locale) {
        return "PolSARPro";
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof File) {
            final File selectedFile = (File) input;
            final String fname = selectedFile.getName().toLowerCase();
            if (fname.equals("config.txt") || fname.endsWith("bin.hdr")) {
                final File folder = selectedFile.getParentFile();
                final File[] files = folder.listFiles();
                if (files != null) {
                    boolean hasConfigFile = false;
                    boolean hasHeaderFile = false;
                    for (File file : files) {
                        final String name = file.getName().toLowerCase();
                        if (name.equals("config.txt")) {
                            hasConfigFile = true;
                        } else if (name.endsWith("bin.hdr")) {
                            hasHeaderFile = true;
                        }
                    }
                    if(hasConfigFile && hasHeaderFile) {
                        return DecodeQualification.INTENDED;
                    }
                }
            }
        }

        return DecodeQualification.UNABLE;
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("T3",
                                               new String[]{
                                                       "T11",
                                                       "T22",
                                                       "T33"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("C3",
                                               new String[]{
                                                       "C11",
                                                       "C22",
                                                       "C33"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("T2",
                                               new String[]{
                                                       "T11",
                                                       "T22",
                                                       "T11/T22"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("C2",
                                               new String[]{
                                                       "C11",
                                                       "C22",
                                                       "C11/C22"
                                               }
        ));
    }
}
