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
package org.esa.nest.util;

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.IOException;

/**
 * loads the nest.config file
 */
public class Config {

    private final PropertyMap prefs = new PropertyMap();
    private static Config instance = null;

    private Config() {
        try {
            prefs.load(new File(SystemUtils.getBeamHomeDir(), "config"+File.separator+SystemUtils.getApplicationContextId()+".config"));
        } catch(IOException e) {
            System.out.println("Unable to load config preferences "+e.getMessage());
        }
    }

    public static PropertyMap getConfigPropertyMap() {
        if(instance == null) {
            instance = new Config();    
        }
        return instance.prefs;
    }
}
