package com.bc.ceres.core;

/**
 * An implementation of a {@link com.bc.ceres.core.ExtensionFactory} for a single extension type.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public class SingleTypeExtensionFactory<T, E> implements ExtensionFactory {
    private final Class<E> extensionType;
    private final Class<? extends E> extensionSubType;

    /**
     * Constructs a {@code SingleTypeExtensionFactory} for extensions of the given type.
     * The {@link #getExtensionSubType() extensionSubType} will be the same as the given type.
     *
     * @param extensionType The extension type.
     */
    public SingleTypeExtensionFactory(Class<E> extensionType) {
        this(extensionType, extensionType);
    }

    /**
     * Constructs a {@code SingleTypeExtensionFactory} for extensions of the given sub-type which implement the given type.
     *
     * @param extensionType    The extension type. Must be {@link Class#isAssignableFrom(Class) assignable from} {@code extensionSubType}.
     * @param extensionSubType The specific extension sub-type.
     */
    public SingleTypeExtensionFactory(Class<E> extensionType, Class<? extends E> extensionSubType) {
        Assert.argument(extensionType.isAssignableFrom(extensionSubType), "extensionType.isAssignableFrom(extensionSubType)");
        this.extensionType = extensionType;
        this.extensionSubType = extensionSubType;
    }

    /**
     * @return The extension type.
     */
    public final Class<E> getExtensionType() {
        return extensionType;
    }

    /**
     * @return The specific extension sub-type.
     */
    public final Class<? extends E> getExtensionSubType() {
        return extensionSubType;
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
    @SuppressWarnings({"unchecked"})
    @Override
    public final E getExtension(Object object, Class<?> extensionType) {
        if (this.extensionType.isAssignableFrom(extensionType)) {
            try {
                return getExtensionImpl((T) object, (Class<E>) extensionType);
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
     * <p>The default implementation returns {@code getExtensionSubType().newInstance()}. Clients may subclass and
     * override this method in order to implement a more sophisticated instance creation.</p>
     *
     * @param object        The object to be extended.
     * @param extensionType The type of the requested extension.
     * @return The extension object.
     * @throws Throwable If an error occurs.
     */
    protected E getExtensionImpl(T object, Class<E> extensionType) throws Throwable {
        return getExtensionSubType().newInstance();
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
