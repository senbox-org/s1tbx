package com.bc.ceres.figure;

import com.bc.ceres.selection.Selection;

import java.awt.geom.Point2D;

/**
 * A selection of figures.
 * <p/>
 * Figures added to this collection will automatically be {@link #isSelected() selected}.
 * When removed, they become deselected.
 * <p/>
 * A figure selection can have a certain {@link #getSelectionLevel() selection level}.
 * When displayed, a figure selections asks its figures for its {@link Figure#createHandles(int) Handle}s
 * at the given selection level.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureSelection extends FigureCollection, Selection {
    int getSelectionLevel();

    void setSelectionLevel(int selectionLevel);

    /**
     * Gets the handles associated with the current selection level.
     * <ol>
     * <li>For a single selection, the handles are the ones created by the selected figure's
     * {@link com.bc.ceres.figure.Figure#createHandles(int)} factory method.</li>
     * <li>For a multiple selection, the handles are the ones created by this figure selection's
     * {@link com.bc.ceres.figure.Figure#createHandles(int)} factory method.</li>
     * <li>If the selection is empty, an empty handle array is returned.</li>
     * </ol>
     *
     * @return The handles associated with the current selection level.
     *
     * @see #getSelectionLevel()
     */
    Handle[] getHandles();

    Handle getSelectedHandle();

    void setSelectedHandle(Handle handle);

    void selectHandle(Point2D point);
}
