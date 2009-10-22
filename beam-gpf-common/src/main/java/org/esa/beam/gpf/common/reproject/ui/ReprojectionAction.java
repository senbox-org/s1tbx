package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;

import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.referencing.operation.MathTransformProvider;
import org.opengis.referencing.operation.MathTransform;

import java.util.Iterator;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @version $Revision: 2535 $ $Date: 2008-07-09 14:10:01 +0200 (Mi, 09 Jul 2008) $
 */
public class ReprojectionAction extends AbstractVisatAction {
    
    //TODO move to an activator or so (mz 10.2009)
//    static {
//        final ServiceRegistry<MathTransformProvider> serviceRegistry = ServiceRegistryFactory.getInstance().getServiceRegistry(MathTransformProvider.class);
//        GeoTools.addFactoryIteratorProvider(new FactoryIteratorProvider() {
//            
//            @Override
//            public <T> Iterator<T> iterator(Class<T> category) {
//                if (category.equals(serviceRegistry.getServiceType()) ) {
//                    return (Iterator<T>) serviceRegistry.getServices().iterator();
//                } else {
//                    return null;
//                }
//            }
//        });
//    }

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = new ReprojectionDialog(false, "Reproject", "reproject", getAppContext());
        }
        dialog.show();
    }
}
