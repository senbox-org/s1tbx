package org.esa.beam.visat.actions.session.dom;

import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.util.HashMap;
import java.util.Map;

public class DomConverterRegistry {

    private static final DomConverterRegistry instance = new DomConverterRegistry();
    private Map<Class<?>, SessionElementDomConverter<?>> converterMap =
            new HashMap<Class<?>, SessionElementDomConverter<?>>(33);

    static {
        instance.setDomConverter(BitmaskDef.class, new BitmaskDefDomConverter());
        instance.setDomConverter(Product.class, new ProductDomConverter());
        instance.setDomConverter(RasterDataNode.class, new RasterDataNodeDomConverter());
    }

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static DomConverterRegistry getInstance() {
        return DomConverterRegistry.instance;
    }

    /**
     * Sets the converter to be used for the specified type.
     *
     * @param type      The type.
     * @param converter The converter.
     */
    public <T> void setDomConverter(Class<? extends T> type, SessionElementDomConverter<T> converter) {
        converterMap.put(type, converter);
    }

    /**
     * Gets the converter registered with the given type.
     *
     * @param type The type.
     *
     * @return The converter or {@code null} if no such exists.
     */
    public SessionElementDomConverter<?> getConverter(Class<?> type) {
        SessionElementDomConverter<?> domConverter = converterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = converterMap.get(type);
        }
        return domConverter;
    }

}
