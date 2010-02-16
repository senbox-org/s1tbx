package com.bc.ceres.swing.figure;

/**
 * Something that knows about a figure editor.
 * Most likely this will be a GUI component.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureEditorAware {
    /**
     * @return The figure editor.
     */
    FigureEditor getFigureEditor();
}
