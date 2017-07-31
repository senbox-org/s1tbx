package org.esa.snap.core.dataio.placemark;

import org.esa.snap.core.datamodel.Placemark;

import java.util.Map;

/**
 * Class bringing a placemark and additional data together
 *
 * @author Marco Peters
 */
public class PlacemarkData {
    private Placemark placemark;
    private Map<String, Object> extraData;

    public PlacemarkData(Placemark placemark) {
        this(placemark, null);
    }

    public PlacemarkData(Placemark placemark, Map<String, Object> extraData) {
        this.placemark = placemark;
        this.extraData = extraData;
    }

    public Placemark getPlacemark() {
        return placemark;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }
}
