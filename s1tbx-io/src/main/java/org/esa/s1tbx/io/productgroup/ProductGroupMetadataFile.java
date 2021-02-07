package org.esa.s1tbx.io.productgroup;

import org.esa.s1tbx.commons.io.JSONUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class ProductGroupMetadataFile {

    private final List<Asset> assetList = new ArrayList<>();

    private static final String ASSETS = "assets";
    private static final String HREF = "href";
    private static final String FORMAT = "snap:format";

    public ProductGroupMetadataFile() {
    }

    public void addAsset(final Asset asset) {
        assetList.add(asset);
    }

    public File write(final File file) throws Exception {
        final JSONObject json = new JSONObject();

        final JSONObject assets = new JSONObject();
        json.put(ASSETS, assets);

        for(Asset asset : assetList) {
            JSONObject assetJSON = new JSONObject();
            assets.put(asset.name, assetJSON);
            assetJSON.put(HREF, asset.path);
            assetJSON.put(FORMAT, asset.format);
        }

        JSONUtils.writeJSON(json, file);
        return file;
    }

    public static class Asset {
        String name;
        String path;
        String format;
        public Asset(final String name, final String path, final String format) {
            this.name = name;
            this.path = path;
            this.format = format;
        }
    }
}
