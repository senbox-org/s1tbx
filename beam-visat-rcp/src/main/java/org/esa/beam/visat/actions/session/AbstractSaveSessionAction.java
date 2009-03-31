package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import java.util.ArrayList;
import java.io.File;
import java.awt.Container;

public abstract class AbstractSaveSessionAction extends ExecCommand {

    private final String title;

    protected AbstractSaveSessionAction(String title) {
        this.title = title;
    }

    public final String getTitle() {
        return title;
    }

    @Override
    public final void actionPerformed(final CommandEvent event) {
        final VisatApp app = VisatApp.getApp();
        final Product[] products = app.getProductManager().getProducts();
        final ArrayList<Product> noFileProducts = new ArrayList<Product>();
        for (Product product : products) {
            if (product.getFileLocation() == null) {
                noFileProducts.add(product);
            }
        }
        if (!noFileProducts.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                    "The session connot be saved because the following" +
                            "products have not been saved yet:\n");
            for (Product product : noFileProducts) {
                sb.append("  ");
                sb.append(product.getDisplayName());
                sb.append("\n");
            }
            app.showErrorDialog(getTitle(), sb.toString());
            return;
        }

        final File sessionFile = getSessionFile(app);
        if (sessionFile == null) {
            return;
        }
        if (sessionFile.equals(app.getSessionFile())) {
            app.setSessionFile(sessionFile);
        }

        try {
            final Session session = createSession(app);
            SessionIO.getInstance().writeSession(session, sessionFile);
        } catch (Exception e) {
            e.printStackTrace();
            app.showErrorDialog(e.getMessage());
        }
    }

    protected abstract File getSessionFile(VisatApp app);

    private Session createSession(VisatApp app) {
        ArrayList<ProductNodeView> nodeViews = new ArrayList<ProductNodeView>();
        final JInternalFrame[] internalFrames = app.getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductNodeView) {
                nodeViews.add((ProductNodeView) contentPane);
            }
        }
        return new Session(app.getProductManager().getProducts(),
                           nodeViews.toArray(new ProductNodeView[nodeViews.size()]));
    }

    @Override
    public final void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getProductManager().getProductCount() > 0);
    }
}
