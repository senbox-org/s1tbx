package org.esa.beam.framework.ui.application;

public interface PageComponentService {
    PageComponent getActiveComponent();

    void addPageComponentListener(PageComponentListener listener);

    void removePageComponentListener(PageComponentListener listener);

}
