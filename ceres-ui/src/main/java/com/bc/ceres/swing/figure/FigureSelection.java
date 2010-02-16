package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.selection.Selection;

/**
 * A selection of figures.
 * <p/>
 * Figures added to this collection will automatically be {@link #isSelected() selected}.
 * When removed, they become deselected.
 * <p/>
 * A figure selection can have a certain {@link #getSelectionStage() selection stage}.
 * When displayed, a figure selections asks its figures for its {@link Figure#createHandles(int) Handle}s
 * at the given stage.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureSelection extends FigureCollection, Selection {
    /**
     * Gets the current selection stage.
     * The maximum selection stage is given by the selected figure(s).
     *
     * @return The current selection stage.
     * @see Figure#getMaxSelectionStage()
     * @see Figure#createHandles(int)
     */
    int getSelectionStage();

    /**
     * Sets the current selection stage.
     * The maximum selection stage is given by the selected figure(s).
     *
     * @param stage The current selection stage.
     */
    void setSelectionStage(int stage);

    /**
     * Gets the handles associated with the current selection stage.
     * <ol>
     * <li>For a single selection, the handles are the ones created by the selected figure's
     * {@link com.bc.ceres.swing.figure.Figure#createHandles(int)} factory method.</li>
     * <li>For a multiple selection, the handles are the ones created by this figure selection's
     * {@link com.bc.ceres.swing.figure.Figure#createHandles(int)} factory method.</li>
     * <li>If the selection is empty, an empty handle array is returned.</li>
     * </ol>
     *
     * @return The handles associated with the current selection stage.
     *
     * @see #getSelectionStage()
     */
    Handle[] getHandles();

    Handle getSelectedHandle();

    void setSelectedHandle(Handle handle);
}
