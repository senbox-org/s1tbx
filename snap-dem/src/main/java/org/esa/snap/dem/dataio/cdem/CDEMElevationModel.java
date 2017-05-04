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
package org.esa.snap.dem.dataio.cdem;

import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.download.downloadablecontent.DownloadableContentImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CDEMElevationModel extends BaseElevationModel {

    private static final String DB_FILE_SUFFIX = ".tif";
    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn("GeoTIFF");
    private static final String tilePrefix = "cdem_dem_";

    private Map<String, List<CDEMFile>> tileMap = new HashMap<>();

    public CDEMElevationModel(final CDEMElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    public final double getElevation(final GeoPos geoPos) throws Exception {

        final double indexY = getIndexY(geoPos);
        final double indexX = getIndexX(geoPos);
        if (indexX < 0 || indexY < 0) {
            return NO_DATA_VALUE;
        }

        final String folder = indexX < 10 ? "0" + (int) indexX + (int) indexY : "" + (int) indexX + (int) indexY;
        List<CDEMFile> cdemFiles = tileMap.get(folder);
        if (cdemFiles == null) {
            loadTiles(folder);
            cdemFiles = tileMap.get(folder);
        }
        if (cdemFiles != null) {
            for (CDEMFile cdemFile : cdemFiles) {
                CDEMElevationTile tile = (CDEMElevationTile) cdemFile.getTile();
                if (tile != null && tile.getTileGeocoding() != null) {
                    final PixelPos pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                    if (pix == null || !pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight()) {
                        continue;
                    }
                    Resampling.Index resamplingIndex = resampling.createIndex();
                    resampling.computeIndex(pix.x, pix.y, tile.getWidth(), tile.getHeight(), resamplingIndex);

                    final double value = resampling.resample(tile, resamplingIndex);
                    if (Double.isNaN(value)) {
                        return descriptor.getNoDataValue();
                    }
                    return value;
                }
            }
        }

        return descriptor.getNoDataValue();
    }

    private void loadTiles(final String folder) {
        final File demInstallDir = descriptor.getDemInstallDir();
        final File demFolder = new File(demInstallDir, folder);

        File[] files = demFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".zip");
            }
        });
        if (files == null || files.length == 0) {
            download(folder, demFolder);
        }
        if (files != null && files.length > 0) {
            List<CDEMFile> tileList = new ArrayList<>();
            for (File file : files) {
                CDEMFile demFile = new CDEMFile(this, file, productReaderPlugIn.createReaderInstance());
                tileList.add(demFile);
            }
            tileMap.put(folder, tileList);
        }
    }

    private void download(final String folder, final File demFolder) {

        List<String> fileList = getFileURLs(CDEMFile.getRemoteHTTP()+ folder, ".zip");
        for(String fileName : fileList) {
            try {
                final File localFile = new File(demFolder, fileName);
                DownloadableContentImpl.getRemoteHttpFile(new URL(CDEMFile.getRemoteHTTP()+ folder +"/"), localFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> getFileURLs(final String path, final String ext) {
        final List<String> fileList = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(path).timeout(10*1000).validateTLSCertificates(false).get();

            final Element table = doc.select("table").first();
            final Elements tbRows = table.select("tr");

            for(Element row : tbRows) {
                Elements tbCols = row.select("td");
                for(Element col : tbCols) {
                    Elements elems = col.getElementsByTag("a");
                    for(Element elem : elems) {
                        String link = elem.text();
                        if(link.endsWith(ext)) {
                            fileList.add(elem.text());
                        }
                    }
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Unable to connect to "+path+ ": "+e.getMessage());
        }
        return fileList;
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return (int) (((geoPos.lon*-1) - 48) / 8);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return (int) ((geoPos.lat - 40) / 4);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 60.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final String fileName = createTileFilename(x + 1, y + 1);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][y] = new CDEMFile(this, localFile, productReaderPlugIn.createReaderInstance());
    }

    private String createTileFilename(final int tileX, final int tileY) {
        final StringBuilder name = new StringBuilder(tilePrefix);
        if (tileX < 10) {
            name.append('0');
        }
        name.append(tileX);
        name.append('_');
        if (tileY < 10) {
            name.append('0');
        }
        name.append(tileY);
        name.append(DB_FILE_SUFFIX);
        return name.toString();
    }

}
