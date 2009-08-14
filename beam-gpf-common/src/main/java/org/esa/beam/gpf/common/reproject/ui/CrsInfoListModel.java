package org.esa.beam.gpf.common.reproject.ui;

import javax.swing.AbstractListModel;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfoListModel extends AbstractListModel {

    private final List<CrsInfo> crsList;

    CrsInfoListModel(List<CrsInfo> projectedCRSList) {
        crsList = new ArrayList<CrsInfo>();
        crsList.addAll(projectedCRSList);
    }

    @Override
    public CrsInfo getElementAt(int index) {
        return crsList.get(index);
    }

    @Override
    public int getSize() {
        return crsList.size();
    }
}
