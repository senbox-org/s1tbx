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
package org.esa.beam.dataio.avhrr;

import java.io.IOException;
import java.util.List;

import org.esa.beam.framework.datamodel.ProductData;

public abstract class AvhrrFile {

	protected int productWidth;

	protected int productHeight;
	
	protected int channel3ab;
	
	protected boolean northbound = false;
	
	abstract public void readHeader() throws IOException;
	
	abstract public String getProductName();
	
	public int getProductWidth() {
		return productWidth;
	}
	
	public int getProductHeight() {
		return productHeight;
	}
	
	abstract public ProductData.UTC getStartDate();
	
	abstract public ProductData.UTC getEndDate();
	
	public int getChannel3abState() {
		return channel3ab;
	}
	
	abstract public List getMetaData();
	
	abstract public BandReader createVisibleRadianceBandReader(int channel);
	
	abstract public BandReader createIrRadianceBandReader(int channel);
	
	abstract public BandReader createIrTemperatureBandReader(int channel);

	abstract public BandReader createReflectanceFactorBandReader(int channel);
	
	abstract public String[] getTiePointNames();
	
	abstract public float[][] getTiePointData() throws IOException;
	
	abstract public int getScanLineOffset(int rawY);

	abstract public int getFlagOffset(int rawY);
		
	public RawCoordinates getRawCoordiantes(int sourceOffsetX,
			int sourceOffsetY, int sourceWidth, int sourceHeight) {
		RawCoordinates coordinates = new RawCoordinates();
		if (northbound) {
			coordinates.minX = productWidth - sourceOffsetX - sourceWidth;
			coordinates.maxX = productWidth - sourceOffsetX - 1;
			coordinates.minY = productHeight - sourceOffsetY - sourceHeight;
			coordinates.maxY = productHeight - sourceOffsetY - 1;
			coordinates.targetStart = sourceWidth * sourceHeight - 1;
			coordinates.targetIncrement = -1;
		} else {
			coordinates.minX = sourceOffsetX;
			coordinates.maxX = sourceOffsetX + sourceWidth - 1;
			coordinates.minY = sourceOffsetY;
			coordinates.maxY = sourceOffsetY + sourceHeight - 1;
			coordinates.targetStart = 0;
			coordinates.targetIncrement = 1;
		}
		coordinates.minX += AvhrrConstants.TP_TRIM_X;
		coordinates.maxX += AvhrrConstants.TP_TRIM_X;
		return coordinates;
	}
	
	public class RawCoordinates {
		public int minX;
		public int maxX;
		public int minY;
		public int maxY;
		public int targetIncrement;
		public int targetStart;
    }
}
