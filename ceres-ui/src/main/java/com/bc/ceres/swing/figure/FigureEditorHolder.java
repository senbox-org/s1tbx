package com.bc.ceres.swing.figure;

/**
 * Something that has a figure editor.
 * Most likely this will be a GUI component.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureEditorHolder {
    /**
     * @return The figure editor.
     */
    FigureEditor getFigureEditor();
}
