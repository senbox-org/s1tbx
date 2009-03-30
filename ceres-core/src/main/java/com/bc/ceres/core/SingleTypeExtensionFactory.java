package com.bc.ceres.core;

/**
 * An abstract implementation of a {@link com.bc.ceres.core.ExtensionFactory} for a single extension type.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public abstract class SingleTypeExtensionFactory<T> implements ExtensionFactory<T> {
    private final Class<?> extensionType;
    private final Class<?> extensionSubType;

    protected SingleTypeExtensionFactory(Class<?> extensionType) {
        this(extensionType, extensionType);
    }

    protected SingleTypeExtensionFactory(Class<?> extensionType, Class<?> extensionSubType) {
        Assert.notNull(extensionType, "extensionType");
        Assert.notNull(extensionType, "extensionSubType");
        this.extensionType = extensionType;
        this.extensionSubType = extensionSubType;
    }

    /**
     * Gets an instance of an extension type for the specified object of type T.
     * If this factory's {@code extensionType} is assignable from the given {@code extensionType}, the method
     * calls {@link #getExtensionImpl(Object, Class)}. Otherwise {@code null} is returned.
     *
     * @param object        The object to be extended.
     * @param extensionType The type of the requested extension.
     * @return The extension object, or {@code null} if the given object is not extensible by this factory or if an error occurs during the call to {@link #getExtensionImpl(Object, Class)}.
     */
    @Override
    public final Object getExtension(T object, Class<?> extensionType) {
        if (this.extensionType.isAssignableFrom(extensionType)) {
            try {
                return getExtensionImpl(object, extensionType);
            } catch (Throwable throwable) {
                // Ignore
            }
        }
        return null;
    }

    /**
     * Creates an extension object for the given {@code object}.
     * The new extension object must be an instance of the {@code extensionSubType} passed to the constructor.
     * Called if, and only if this factory's {@code extensionType} is assignable from the given {@code extensionType}.
     *
     * @param object        The object to be extended.
     * @param extensionType The type of the requested extension.
     * @return The extension object.
     * @throws Throwable If an error occurs.
     */
    protected abstract Object getExtensionImpl(T object, Class<?> extensionType) throws Throwable;

    /**
     * @return The general extension type.
     */
    public final Class<?> getExtensionType() {
        return extensionType;
    }

    /**
     * @return The specific extension sub-type.
     */
    public Class<?> getExtensionSubType() {
        return extensionSubType;
    }

    /**
     * @return The one-element array containing the extension sub-type this factory supports.
     */
    @Override
    public final Class<?>[] getExtensionTypes() {
        if (extensionType.equals(extensionSubType)) {
            return new Class<?>[]{getExtensionSubType()};
        }
        return new Class<?>[]{getExtensionSubType(), getExtensionType()};
    }

}
