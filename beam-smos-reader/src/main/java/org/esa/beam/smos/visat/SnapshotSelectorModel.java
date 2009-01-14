/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.smos.visat;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

class SnapshotSelectorModel {
    private final SpinnerListModel spinnerModel;
    private final DefaultBoundedRangeModel sliderModel;

    SnapshotSelectorModel(Integer[] snapshotIds) {
        this(Arrays.asList(snapshotIds));
    }

    SnapshotSelectorModel(List<Integer> snapshotIdList) {
        spinnerModel = new SpinnerListModel(snapshotIdList);
        sliderModel = new DefaultBoundedRangeModel(0, 0, 0, snapshotIdList.size() - 1);

        spinnerModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final SpinnerListModel spinnerListModel = (SpinnerListModel) e.getSource();
                @SuppressWarnings("unchecked")
                final List<Integer> list = (List<Integer>) spinnerListModel.getList();
                @SuppressWarnings("unchecked")
                final Integer spinnerValue = (Integer) spinnerListModel.getValue();
                final int sliderValue = Collections.binarySearch(list, spinnerValue);

                sliderModel.setValue(sliderValue);
            }
        });

        sliderModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final BoundedRangeModel sliderModel = (BoundedRangeModel) e.getSource();
                final int sliderValue = sliderModel.getValue();
                final Object spinnerValue = spinnerModel.getList().get(sliderValue);

                spinnerModel.setValue(spinnerValue);
            }
        });
    }

    SpinnerModel getSpinnerModel() {
        return spinnerModel;
    }

    BoundedRangeModel getSliderModel() {
        return sliderModel;
    }

    String getSliderInfo() {
        return MessageFormat.format("{0} / {1}", sliderModel.getValue() + 1, sliderModel.getMaximum() + 1);
    }
}
