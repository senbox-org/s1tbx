package org.esa.beam.framework.gpf;

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.RasterImpl;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;
import org.esa.beam.framework.dataio.ProductReader;

import com.bc.ceres.core.Assert;

/**
 * A tile.
 */
public class Tile {

	public final static int DEFAULT_TILE_SIZE = 200;
	
    public enum State {
        NOT_COMPUTED,
        COMPUTING,
        COMPUTED,
        ERROR
    }

    private long timeStamp;
    private long memorySize;
    private State state;
    private int queryCount;
    private Raster raster;
    private Rectangle rectangle;

	public Tile(Rectangle rectangle) {
        this.state = State.NOT_COMPUTED;
        this.rectangle = rectangle;        
    }
	
	public Raster getRaster() {
		return raster;
	}

	public void setRaster(Raster raster) {
		this.raster = raster;
		ProductData dataBuffer = raster.getDataBuffer();
		this.memorySize = (long) dataBuffer.getNumElems() * (long) dataBuffer.getElemSize();
		this.timeStamp = System.currentTimeMillis();
	}
	
	public Rectangle getRectangle() {
		return rectangle;
	}

	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	public State getState() {
        return state;
    }

    public void setState(State state) {
        Assert.notNull(state);
        this.state = state;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public void incQueryCount() {
        queryCount++;
        timeStamp = System.currentTimeMillis();
    }
    
    public static Tile createTile(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer) { 
    	if (dataBuffer == null) {
            dataBuffer = rasterDataNode.createCompatibleRasterData(tileRectangle.width, tileRectangle.height);
        }
        Raster raster = new RasterImpl(rasterDataNode, tileRectangle, dataBuffer);
        Tile tile = new Tile(tileRectangle);
        tile.setRaster(raster);
        return tile;
    }

    @Override
    public String toString() {
        RasterDataNode rasterDataNode = getRaster().getRasterDataNode();
        StringBuilder sb = new StringBuilder();
        sb.append("Tile[");
        sb.append("rect=");
        sb.append(getRectangle());
        sb.append(",state=");
        sb.append(getState());
        sb.append(",band=");
        sb.append(rasterDataNode.getName());
        sb.append(",reader=");
        sb.append(rasterDataNode.getProductReader());
        sb.append("]");
        return sb.toString();
    }
}
