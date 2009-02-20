package org.esa.beam.framework.ui.mpage;

import java.awt.Window;

public interface PageContext {

    Window getWindow();

    Page getCurrentPage();

    void setCurrentPage(Page page);

    void updateState();

    void showErrorDialog(String message);
}
