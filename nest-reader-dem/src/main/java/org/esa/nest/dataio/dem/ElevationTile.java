package org.esa.nest.dataio.dem;

/**

 */
public interface ElevationTile {

    public void dispose();

    public float getSample(int pixelX, int pixelY) throws Exception;

    public void clearCache();
}
