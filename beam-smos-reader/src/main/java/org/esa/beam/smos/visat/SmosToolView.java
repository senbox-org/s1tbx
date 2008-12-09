package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;

public abstract class SmosToolView extends AbstractToolView {

    private JPanel panel;
    private JLabel defaultComponent;
    private JComponent clientComponent;
    private SmosToolView.SVSL svsl;

    protected SmosToolView() {
    }

    public ProductSceneView getSelectedSmosView() {
        return SmosBox.getInstance().getSmosViewSelectionService().getSelectedSceneView();
    }

    public Product getSelectedSmosProduct() {
        return SmosBox.getInstance().getSmosViewSelectionService().getSelectedSmosProduct();
    }

    public SmosFile getSelectedSmosFile() {
        return SmosBox.getInstance().getSmosViewSelectionService().getSelectedSmosFile();
    }

    @Override
    protected JComponent createControl() {
        panel = new JPanel(new BorderLayout());
        URL resource = getClass().getResource("smos-icon.png");
        if (resource != null) {
            defaultComponent = new JLabel(new ImageIcon(resource));
        } else {
            defaultComponent = new JLabel();
        }
        defaultComponent.setIconTextGap(10);
        defaultComponent.setText("No SMOS image selected.");
        panel.add(defaultComponent);

        super.getContext().getPage().addPageComponentListener(new PageComponentListenerAdapter() {
            @Override
            public void componentOpened(PageComponent component) {
                super.componentOpened(component);
            }

            @Override
            public void componentClosed(PageComponent component) {
                super.componentClosed(component);
            }

            @Override
            public void componentShown(PageComponent component) {
                super.componentShown(component);
            }

            @Override
            public void componentHidden(PageComponent component) {
                super.componentHidden(component);
            }
        });

        return panel;
    }

    @Override
    public void componentOpened() {
        svsl = new SVSL();
        SmosBox.getInstance().getSmosViewSelectionService().addSceneViewSelectionListener(svsl);
        realizeSmosView(SmosBox.getInstance().getSmosViewSelectionService().getSelectedSceneView());
    }

    @Override
    public void componentClosed() {
        SmosBox.getInstance().getSmosViewSelectionService().removeSceneViewSelectionListener(svsl);
        realizeSmosView(null);
    }

    @Override
    public void componentShown() {
        realizeSmosView(SmosBox.getInstance().getSmosViewSelectionService().getSelectedSceneView());
    }

    @Override
    public void componentHidden() {
        realizeSmosView(null);
    }

    protected void realizeSmosView(ProductSceneView newView) {
        if (newView != null) {
            if (clientComponent == null) {
                clientComponent = createClientComponent(newView);
            }
            setToolViewComponent(clientComponent);
            updateClientComponent(newView);
        } else {
            setToolViewComponent(defaultComponent);
        }
    }

    protected abstract JComponent createClientComponent(ProductSceneView smosView);

    protected abstract void updateClientComponent(ProductSceneView smosView);

    public static Number getNumbericMember(CompoundData compoundData, int memberIndex) throws IOException {
        Type memberType = compoundData.getCompoundType().getMemberType(memberIndex);
        Number number;
        if (memberType == SimpleType.DOUBLE) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.FLOAT) {
            number = compoundData.getFloat(memberIndex);
        } else if (memberType == SimpleType.ULONG) {
            // This mask is used to obtain the value of an int as if it were unsigned.
            BigInteger mask = BigInteger.valueOf(0xffffffffffffffffL);
            BigInteger bi = BigInteger.valueOf(compoundData.getLong(memberIndex));
            number = bi.and(mask);
        } else if (memberType == SimpleType.LONG || memberType == SimpleType.UINT) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.INT || memberType == SimpleType.USHORT) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.SHORT || memberType == SimpleType.UBYTE) {
            number = compoundData.getDouble(memberIndex);
        } else if (memberType == SimpleType.BYTE) {
            number = compoundData.getDouble(memberIndex);
        } else {
            number = null;
        }
        return number;
    }

    public static Class<? extends Number> getNumbericMemberType(CompoundType compoundData, int memberIndex) {
        Type memberType = compoundData.getMemberType(memberIndex);
        Class<? extends Number> numberClass;
        if (memberType == SimpleType.DOUBLE) {
            numberClass = Double.class;
        } else if (memberType == SimpleType.FLOAT) {
            numberClass = Float.class;
        } else if (memberType == SimpleType.ULONG) {
            numberClass = BigInteger.class;
        } else if (memberType == SimpleType.LONG || memberType == SimpleType.UINT) {
            numberClass = Long.class;
        } else if (memberType == SimpleType.INT || memberType == SimpleType.USHORT) {
            numberClass = Integer.class;
        } else if (memberType == SimpleType.SHORT || memberType == SimpleType.UBYTE) {
            numberClass = Short.class;
        } else if (memberType == SimpleType.BYTE) {
            numberClass = Byte.class;
        } else {
            numberClass = null;
        }
        return numberClass;
    }

    private void setToolViewComponent(JComponent comp) {
        panel.removeAll();
        panel.add(comp, BorderLayout.CENTER);
        panel.invalidate();
        panel.validate();
    }

    private class SVSL implements SceneViewSelectionService.SelectionListener {
        @Override
        public void handleSceneViewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
            realizeSmosView(newView);
        }
    }
}