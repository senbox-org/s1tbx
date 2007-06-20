package org.esa.beam.visat.toolviews.stat;

import java.awt.Container;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import com.bc.layer.LayerModelChangeAdapter;
import com.bc.layer.LayerModelChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 25.10.2005
 * Time: 11:18:52
 */

/**
 * Description of LayerObserver
 *
 * @author Marco Peters
 */
class LayerObserver {

    private static final Map<Class, LayerObserver> _observerMap = new Hashtable<Class, LayerObserver>();

    private final Class _layerClass;

    private RasterDataNode _raster;
    private List<LayerObserverListener> _listeners;
    private LayerModelChangeAdapter _layerListener;


    /**
     * Gets the instance for the given <code>layerClass</code>.
     *
     * @param layerClass The layer which shopuld be observed.
     *
     * @return An instance of <code>LayerObserver</code>.
     */
    public static LayerObserver getInstance(final Class layerClass) {
        Debug.assertTrue(Layer.class.isAssignableFrom(layerClass), "layerClass does not implement Layer interface");
        if (!_observerMap.containsKey(layerClass)) {
            _observerMap.put(layerClass, new LayerObserver(layerClass));
        }
        return _observerMap.get(layerClass);
    }

    // hide the construct cause of singelton class
    private LayerObserver(final Class layerClass) {
        _listeners = new ArrayList<LayerObserverListener>();
        _listeners.listIterator();
        _layerClass = layerClass;
    }

    /**
     * Adds a {@link LayerObserverListener}.
     *
     * @param listener The listener to add.
     */
    public void addLayerObserverListener(final LayerObserverListener listener) {
        if (!_listeners.contains(listener)) {
            _listeners.add(listener);
        }
    }

    /**
     * Removes a {@link LayerObserverListener}.
     *
     * @param listener The listener to remove.
     */
    public void removeLayerObserverListener(final LayerObserverListener listener) {
        if (_listeners.contains(listener)) {
            _listeners.remove(listener);
        }
    }

    /**
     * Sets the current raster which layer is observed.
     *
     * @param raster A raster.
     */
    public void setRaster(final RasterDataNode raster) {
        if (raster != _raster) {
            removeLayerModelListenerForRaster(_raster);
            _raster = raster;
            addLayerModelListenerForRaster(raster);
            fireLayerChanged();
        }
    }

    private void removeLayerModelListenerForRaster(final RasterDataNode raster) {
        final JInternalFrame internalFrame = VisatApp.getApp().findInternalFrame(raster);
        if (internalFrame != null) {
            final Container oldContentPane = internalFrame.getContentPane();
            if (oldContentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) oldContentPane;
                sceneView.getImageDisplay().getLayerModel().removeLayerModelChangeListener(getLayerModelListener());
            }
        }
    }

    private LayerModelChangeListener getLayerModelListener() {
        if (_layerListener == null) {
            _layerListener = new LayerModelChangeAdapter() {
                @Override
                public void handleLayerChanged(final LayerModel layerModel, final Layer layer) {
                    if (_layerClass.isInstance(layer)) {
                        fireLayerChanged();
                    }
                }
            };
        }
        return _layerListener;
    }

    private void fireLayerChanged() {
        for (final LayerObserverListener listener : _listeners) {
            listener.layerChanged();
        }
    }

    private void addLayerModelListenerForRaster(final RasterDataNode raster) {
        final VisatApp app = VisatApp.getApp();
        final JInternalFrame internalFrame = app.findInternalFrame(raster);
        if (internalFrame != null) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) contentPane;
                if (ArrayUtils.isMemberOf(raster, sceneView.getRasters())) {
                    sceneView.getImageDisplay().getLayerModel().addLayerModelChangeListener(getLayerModelListener());
                }
            }
        } else {
            app.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameOpened(final InternalFrameEvent e) {
                    final Container contentPane = e.getInternalFrame().getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        final ProductSceneView sceneView = (ProductSceneView) contentPane;
                        if (ArrayUtils.isMemberOf(raster, sceneView.getRasters())) {
                            sceneView.getImageDisplay().getLayerModel().addLayerModelChangeListener(
                                    getLayerModelListener());
                            app.removeInternalFrameListener(this);
                        }
                    }
                }
            });
        }
    }

    /**
     * The listener can be added to a LayerObserver to be informed about changes of the observed layer.
     */
    public interface LayerObserverListener extends EventListener {

        /**
         * Indicates that the observed layer has changed.
         */
        public void layerChanged();
    }

}
