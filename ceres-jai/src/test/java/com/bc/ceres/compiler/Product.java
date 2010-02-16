package com.bc.ceres.compiler;

import java.util.HashMap;


public class Product {
    HashMap<String,Band> bandMap;

    public Product(Band[] bands) {
        bandMap  = new HashMap<String, Band>();
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            bandMap.put(band.getName(), band);
        }
    }

    public Band getBand(String name) {
        return bandMap.get(name);
    }
}
