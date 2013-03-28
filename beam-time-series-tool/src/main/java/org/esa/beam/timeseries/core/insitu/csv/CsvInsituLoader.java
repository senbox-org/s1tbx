/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.timeseries.core.insitu.csv;

import com.bc.ceres.core.Assert;
import org.esa.beam.timeseries.core.insitu.InsituLoader;
import org.esa.beam.timeseries.core.insitu.RecordSource;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;

/**
 * Implementation of {@link InsituLoader} reading in situ data from a csv source.
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class CsvInsituLoader implements InsituLoader {

    private Reader reader;
    private DateFormat dateFormat;

    @Override
    public RecordSource loadSource() throws IOException {
        Assert.state(reader != null, "reader != null");
        Assert.state(dateFormat != null, "dateFormat != null");
        return new CsvRecordSource(reader, dateFormat);
    }

    public void setCsvReader(Reader reader) {
        this.reader = reader;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }
}
