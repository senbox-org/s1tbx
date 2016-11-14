package org.jlinda.nest.utils;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;

/**
* User: pmar@ppolabs.com
* Date: 6/20/11
* Time: 11:16 PM
*/
public class CplxContainer {

    public String name;
    public String date;
    public SLCImage metaData;
    public Orbit orbit;
    public Band realBand;
    public Band imagBand;
    public String polarisation;
    public String subswath;

    public CplxContainer(String date, SLCImage metaData, Orbit orbit, Band realBand, Band imagBand) {
        this.date = date;
        this.metaData = metaData;
        this.orbit = orbit;
        this.realBand = realBand;
        this.imagBand = imagBand;
        this.polarisation = OperatorUtils.getPolarizationFromBandName(realBand.getName());
        this.subswath = OperatorUtils.getSubswathFromBandName(realBand.getName());
        this.name = realBand.getName();
    }
}
