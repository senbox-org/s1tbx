/*
 * $Id: ViewPane.java,v 1.2 2006/10/23 06:38:14 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package com.bc.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bc.view.ScrollableView;
import com.bc.view.ViewModel;
import com.bc.view.ViewModelChangeListener;

/**
 * A <code>ViewPane</code> is an alternative to {@link javax.swing.JScrollPane}
 * when you need to scroll an infinite area given in floating-point coordinates.
 * <p>
 * In opposite to {@link javax.swing.JScrollPane}, we don't scroll a view  given
 * by a {@link javax.swing.JComponent} but it's {@link com.bc.view.ViewModel}. For this reason
 * the view component must implement the {@link com.bc.view.ScrollableView} interface.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class ViewPane extends JComponent {
    private static final long serialVersionUID = -2634482999458990218L;

    private static final int MAX_SB_VALUE = 10000;

    private JScrollBar horizontalScrollBar;
    private JScrollBar verticalScrollBar;
    private JComponent cornerComponent;
    private JComponent viewComponent;
    private ViewModel viewModel;
    private Rectangle2D scrollArea;
    private boolean updatingScrollBars;
    private boolean updatingViewModel;
    private boolean scrollBarsUpdated;
    private ViewModelChangeHandler viewModelHandler;

    /**
     * Constructs a new view pane with an empty view component.
     */
    public ViewPane() {
        this(null);
    }

    /**
     * Constructs a new view pane with the given view viewComponent
     * @param viewComponent the view viewComponent. If not null, it must implement {@link com.bc.view.ScrollableView}
     */
    public ViewPane(JComponent viewComponent) {
        super();
        scrollArea = new Rectangle2D.Double();
        viewModelHandler = new ViewModelChangeHandler();
        setOpaque(true);
        setLayout(null);
        setViewComponent(viewComponent);
        setCornerComponent(createCornerComponent());
        final ChangeListener scrollBarCH = new ScrollBarChangeHandler();
        horizontalScrollBar = createHorizontalScrollbar();
        horizontalScrollBar.setVisible(false);
        horizontalScrollBar.getModel().addChangeListener(scrollBarCH);
        verticalScrollBar = createVerticalScrollBar();
        verticalScrollBar.setVisible(false);
        verticalScrollBar.getModel().addChangeListener(scrollBarCH);
        add(horizontalScrollBar);
        add(verticalScrollBar);
        addComponentListener(new ViewPaneResizeHandler());
        scrollBarsUpdated = false;
    }

    public JComponent getViewComponent() {
        return viewComponent;
    }

    /**
     * Constructs a new view pane with the given view viewComponent
     * @param viewComponent the view viewComponent. If not null, it must implement {@link com.bc.view.ScrollableView}
     */
    public void setViewComponent(JComponent viewComponent) {
        if (this.viewModel != null) {
            this.viewModel.removeViewModelChangeListener(viewModelHandler);
        }
        if (this.viewComponent != null) {
            remove(this.viewComponent);
        }
        this.viewComponent = viewComponent;
        this.viewModel = null;
        if (this.viewComponent != null) {
            this.viewModel = ((ScrollableView) viewComponent).getViewModel();
            add(this.viewComponent);
        }
        if (this.viewModel != null) {
            this.viewModel.addViewModelChangeListener(viewModelHandler);
        }
        revalidate();
        validate();
    }

    public JComponent getCornerComponent() {
        return cornerComponent;
    }

    public void setCornerComponent(JComponent cornerComponent) {
        if (this.cornerComponent != null) {
            remove(this.cornerComponent);
        }
        this.cornerComponent = cornerComponent;
        if (this.cornerComponent != null) {
            add(this.cornerComponent);
        }
        revalidate();
        validate();
    }

    @Override
    public void doLayout() {
        if (viewComponent == null || !viewComponent.isVisible()) {
            return;
        }
        if (!scrollBarsUpdated) {
            updateScrollBarVisiblityAndValues();
            updateScrollBarIncrements();
        }

        final Insets insets = getInsets();
        final int width = getWidth() - (insets.left + insets.right);
        final int height = getHeight() - (insets.top + insets.bottom);
        if (width <= 0 || height <= 0) {
            return;
        }


        // x1              w1              x2 w2
        // +-------------------------------+---+ y1
        // |                               |   |
        // |                               | v |
        // |                               | s |
        // |                               | b |
        // |             view              |   | h1
        // |                               |   |
        // |                               |   |
        // |                               |   |
        // |                               |   |
        // +-------------------------------+---+ y2
        // |             hsb               |   | h2
        // +-------------------------------+---+
        //
        if (horizontalScrollBar.isVisible() && verticalScrollBar.isVisible()) {
            final Dimension hsbSize = horizontalScrollBar.getPreferredSize();
            final Dimension vsbSize = verticalScrollBar.getPreferredSize();
            final int x1 = insets.left;
            final int y1 = insets.top;
            final int w2 = vsbSize.width;
            final int h2 = hsbSize.height;
            final int w1 = width - w2;
            final int h1 = height - h2;
            final int x2 = x1 + w1;
            final int y2 = y1 + h1;
            viewComponent.setBounds(x1, y1, w1, h1);
            verticalScrollBar.setBounds(x2, y1, w2, h1);
            horizontalScrollBar.setBounds(x1, y2, w1, h2);
            if (cornerComponent != null) {
                cornerComponent.setBounds(x2, y2, w2, h2);
            }
        } else if (horizontalScrollBar.isVisible()) {
            final Dimension hsbSize = horizontalScrollBar.getPreferredSize();
            final int x1 = insets.left;
            final int y1 = insets.top;
            final int w1 = width;
            final int h2 = hsbSize.height;
            final int h1 = height - h2;
            final int y2 = y1 + h1;
            viewComponent.setBounds(x1, y1, w1, h1);
            horizontalScrollBar.setBounds(x1, y2, w1, h2);
        } else if (verticalScrollBar.isVisible()) {
            final Dimension vsbSize = verticalScrollBar.getPreferredSize();
            final int x1 = insets.left;
            final int y1 = insets.top;
            final int w2 = vsbSize.width;
            final int w1 = width - w2;
            final int h1 = height;
            final int x2 = x1 + w1;
            viewComponent.setBounds(x1, y1, w1, h1);
            verticalScrollBar.setBounds(x2, y1, w2, h1);
        } else {
            final int x1 = insets.left;
            final int y1 = insets.top;
            viewComponent.setBounds(x1, y1, width, height);
        }

        viewComponent.doLayout();
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp != horizontalScrollBar &&
                comp != verticalScrollBar &&
                comp != cornerComponent &&
                comp != viewComponent) {
            throw new IllegalArgumentException();
        }
        super.addImpl(comp, constraints, index);
    }

    /**
     * @return <code>new JScrollBar(JScrollBar.HORIZONTAL)</code>
     */
    protected JScrollBar createHorizontalScrollbar() {
        return new JScrollBar(JScrollBar.HORIZONTAL);
    }

    /**
     * @return <code>new JScrollBar(JScrollBar.VERTICAL)</code>
     */
    protected JScrollBar createVerticalScrollBar() {
        return new JScrollBar(JScrollBar.VERTICAL);
    }

    /**
     * @return <code>new JPanel()</code>
     */
    protected JPanel createCornerComponent() {
        return new JPanel();
    }


    private void updateViewModel() {
        if (updatingScrollBars || viewModel == null) {
//            System.out.println("updateViewModel: no!");
            return;
        }

        double mpX = viewModel.getModelOffsetX();
        double mpY = viewModel.getModelOffsetY();
        final boolean hsbVisible = horizontalScrollBar.isVisible();
        final boolean vsbVisible = verticalScrollBar.isVisible();
        final Rectangle2D sa = scrollArea;
        if (hsbVisible) {
            final int hsbValue = horizontalScrollBar.getValue();
            mpX = sa.getX() + hsbValue * sa.getWidth() / MAX_SB_VALUE;
        }
        if (vsbVisible) {
            final int vsbValue = verticalScrollBar.getValue();
            mpY = sa.getY() + vsbValue * sa.getHeight() / MAX_SB_VALUE;
        }
        if (hsbVisible || vsbVisible) {
//            System.out.println("updateViewModel: mpX = " + mpX + ", mpY = " + mpY);
            updatingViewModel = true;
            viewModel.setModelOffset(mpX, mpY);
            updatingViewModel = false;
        }
    }

    private void updateScrollBarVisiblityAndValues() {
        if (updatingViewModel || viewComponent == null || viewModel == null) {
//            System.out.println("updateScrollBars: no!");
            return;
        }

        final int width = viewComponent.getWidth();
        final int height = viewComponent.getHeight();
        if (width <= 0 || height <= 0) {
            horizontalScrollBar.setVisible(false);
            verticalScrollBar.setVisible(false);
            return;
        }

        final Rectangle2D ma = viewModel.getModelArea();
        final double vs = viewModel.getViewScale();
        final Rectangle2D va = new Rectangle2D.Double(viewModel.getModelOffsetX(),
                                                      viewModel.getModelOffsetY(),
                                                      width / vs,
                                                      height / vs);
        final Rectangle2D sa = scrollArea;

        //  x1,x2,y1,y2(+)   no scrollbars         x1,x2,y1,y2(-)  V+H-scrollbars
        //  +--------------------------------+     +--------------------------------+
        //  | va             ^               |     | ma             ^               |
        //  |                |               |     |                |               |
        //  |                y1              |     |                y1              |
        //  |                |               |     |                |               |
        //  |                v               |     |                v               |
        //  |        +--------------+        |     |        +--------------+        |
        //  |        | ma           |        |     |        | va           V        |
        //  |<--x1-->|              |<--x2-->|     |<--x1-->|              V<--x2-->|
        //  |        |              |        |     |        |              V        |
        //  |        +--------------+        |     |        +HHHHHHHHHHHHHH+        |
        //  |                ^               |     |                ^               |
        //  |                |               |     |                |               |
        //  |                y2              |     |                y2              |
        //  |                |               |     |                |               |
        //  |                v               |     |                v               |
        //  +--------------------------------+     +--------------------------------+
        //
        //  x1,y1,y2(+) x2(-)  H-scrollbar                 x1,y1,y2(+) x2(-)  H,V-scrollbars
        //  +--------------------------------+             +--------------------------------+
        //  | va                     ^       |             | ma                     ^       |
        //  |                        |       |             |                        |       |
        //  |                        y1      |             |                        y1      |
        //  |                        |       |<--x2-->     |                        |       |<--x2-->
        //  |                        v       |             |                        v       |
        //  |                     +-------------------+    |                     +-------------------+
        //  |                     | ma                |    |                     | va                V
        //  |<---------x1-------->|                   |    |<---------x1-------->|                   V
        //  |                     |                   |    |                     |                   V
        //  |                     +-------------------+    |                     +HHHHHHHHHHHHHHHHHHH+
        //  |                        ^       |             |                        ^       |
        //  |                        |       |             |                        |       |
        //  |                        y2      |             |                        y2      |
        //  |                        |       |             |                        |       |
        //  |                        v       |             |                        v       |
        //  +HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH+             +--------------------------------+
        //
        final double dx1 = ma.getX() - va.getX();
        final double dy1 = ma.getY() - va.getY();
        final double dx2 = (va.getX() + va.getWidth()) - (ma.getX() + ma.getWidth());
        final double dy2 = (va.getY() + va.getHeight()) - (ma.getY() + ma.getHeight());

        boolean hsbVisible = false;
        boolean vsbVisible = false;
        sa.setRect(ma);

        if (dx1 < 0 && dx2 < 0) {
            hsbVisible = true;
        } else if (dx1 < 0) {
            hsbVisible = true;
            sa.setRect(sa.getX(), sa.getY(), sa.getWidth() + dx2, sa.getHeight());
        } else if (dx2 < 0) {
            hsbVisible = true;
            sa.setRect(sa.getX() - dx1, sa.getY(), sa.getWidth() + dx1, sa.getHeight());
        }

        if (dy1 < 0 && dy2 < 0) {
            vsbVisible = true;
        } else if (dy1 < 0) {
            vsbVisible = true;
            sa.setRect(sa.getX(), sa.getY(), sa.getWidth(), sa.getHeight() + dy2);
        } else if (dy2 < 0) {
            vsbVisible = true;
            sa.setRect(sa.getX(), sa.getY() - dy1, sa.getWidth(), sa.getHeight() + dy1);
        }

//        System.out.println("updateScrollBars:");
//        System.out.println("  hsbVisible = " + vsbVisible);
//        System.out.println("  hsbVisible = " + hsbVisible);
//        System.out.println("  va = " + va);
//        System.out.println("  ma = " + ma);
//        System.out.println("  sa = " + sa);
//        System.out.println("  dx1 = " + dx1 + ", dx2 = " + dx2);
//        System.out.println("  dy1 = " + dy1 + ", dy2 = " + dy2);
//        System.out.println("  ");

        updatingScrollBars = true;

        if (hsbVisible) {
            int hsbValue = (int) Math.round(MAX_SB_VALUE * (va.getX() - sa.getX()) / sa.getWidth());
            hsbValue = clamp(hsbValue, 0, MAX_SB_VALUE);
            int hsbExtend = (int) Math.round(MAX_SB_VALUE * va.getWidth() / sa.getWidth());
            hsbExtend = clamp(hsbExtend, 0, MAX_SB_VALUE);
            horizontalScrollBar.setValues(hsbValue, hsbExtend, 0, MAX_SB_VALUE);
        }
        horizontalScrollBar.setVisible(hsbVisible);

        if (vsbVisible) {
            int vsbValue = (int) Math.round(MAX_SB_VALUE * (va.getY() - sa.getY()) / sa.getHeight());
            vsbValue = clamp(vsbValue, 0, MAX_SB_VALUE);
            int vsbExtend = (int) Math.round(MAX_SB_VALUE * va.getHeight() / sa.getHeight());
            vsbExtend = clamp(vsbExtend, 0, MAX_SB_VALUE);
            verticalScrollBar.setValues(vsbValue, vsbExtend, 0, MAX_SB_VALUE);
        }
        verticalScrollBar.setVisible(vsbVisible);

        if (cornerComponent != null) {
            cornerComponent.setVisible(hsbVisible && vsbVisible);
        }

        updatingScrollBars = false;
        scrollBarsUpdated = true;
    }

    private void updateScrollBarIncrements() {
        // we could set more reasonable increments at this place, e.g. using the viewModel.modelArea property
        horizontalScrollBar.setUnitIncrement(Math.max(10, MAX_SB_VALUE / 50));
        horizontalScrollBar.setBlockIncrement(Math.max(10, MAX_SB_VALUE / 5));
        verticalScrollBar.setUnitIncrement(Math.max(10, MAX_SB_VALUE / 50));
        verticalScrollBar.setBlockIncrement(Math.max(10, MAX_SB_VALUE / 5));
    }

    private static int clamp(int value, int min, int max) {
        return (value < min) ? min : (value > max) ? max : value;
    }

    private class ScrollBarChangeHandler implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            updateViewModel();
        }
    }

    private class ViewPaneResizeHandler extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            updateScrollBarVisiblityAndValues();
        }
    }

    private class ViewModelChangeHandler implements ViewModelChangeListener {
        public void handleViewModelChanged(ViewModel viewModel) {
            updateScrollBarVisiblityAndValues();
            updateScrollBarIncrements();
        }
    }
}
