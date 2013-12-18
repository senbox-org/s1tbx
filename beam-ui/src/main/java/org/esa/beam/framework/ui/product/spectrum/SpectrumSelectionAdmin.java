package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.swing.TristateCheckBox;

import java.util.ArrayList;
import java.util.List;

public class SpectrumSelectionAdmin {

    private List<List<Boolean>> bandSelectionStates;
    private List<Integer> numbersOfSelectedBands;
    private List<Integer> currentStates;

    SpectrumSelectionAdmin() {
        bandSelectionStates = new ArrayList<List<Boolean>>();
        numbersOfSelectedBands = new ArrayList<Integer>();
        currentStates = new ArrayList<Integer>();
    }

    void addSpectrumSelections(DisplayableSpectrum spectrum) {
        List<Boolean> selected = new ArrayList<Boolean>();
        int numberOfSelectedBands = 0;
        for (int i = 0; i < spectrum.getSpectralBands().length; i++) {
            final boolean bandSelected = spectrum.isBandSelected(i);
            selected.add(bandSelected);
            if (bandSelected) {
                numberOfSelectedBands++;
            }
        }
        bandSelectionStates.add(selected);
        numbersOfSelectedBands.add(numberOfSelectedBands);
        currentStates.add(-1);
        evaluate(bandSelectionStates.size() - 1);
    }

    boolean isBandSelected(int row, int i) {
        if (currentStates.get(row) == TristateCheckBox.STATE_MIXED) {
            return bandSelectionStates.get(row).get(i);
        } else return currentStates.get(row) == TristateCheckBox.STATE_SELECTED;
    }

    private void evaluate(int index) {
        final Integer numberOfBands = numbersOfSelectedBands.get(index);
        if (numberOfBands == 0) {
            currentStates.set(index, TristateCheckBox.STATE_UNSELECTED);
        } else if (numberOfBands == bandSelectionStates.get(index).size()) {
            currentStates.set(index, TristateCheckBox.STATE_SELECTED);
        } else {
            currentStates.set(index, TristateCheckBox.STATE_MIXED);
        }
    }

    public int getState(int index) {
        return currentStates.get(index);
    }

    boolean isSpectrumSelected(int row) {
        return currentStates.get(row) != TristateCheckBox.STATE_UNSELECTED;
    }

    void updateBandSelections(int row, int bandRow, boolean selected) {
        bandSelectionStates.get(row).set(bandRow, selected);
        updateNumbersOfSelectedBands(selected, row);
        evaluate(row);
    }

    void updateState(int row, int newState) {
        if (newState == TristateCheckBox.STATE_MIXED) {
            if (numbersOfSelectedBands.get(row) == bandSelectionStates.get(row).size() ||
                    numbersOfSelectedBands.get(row) == 0) {
                newState = TristateCheckBox.STATE_UNSELECTED;
            }
        }
        currentStates.set(row, newState);
    }

    private void updateNumbersOfSelectedBands(Boolean selected, int row) {
        if (selected) {
            numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) + 1);
        } else {
            numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) - 1);
        }
    }

}
