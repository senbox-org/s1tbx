package org.esa.beam.framework.ui.crs;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfoListModel extends AbstractListModel {

    private final List<CrsInfo> crsList;

    CrsInfoListModel(List<CrsInfo> crsList) {
        this.crsList = new ArrayList<CrsInfo>(crsList);
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
