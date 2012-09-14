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
