/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core;

import java.lang.reflect.Constructor;

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
                throw new RuntimeException(throwable);
            }
        }
        return null;
    }

    /**
     * Creates an extension object for the given {@code object}.
     * The new extension object must be an instance of the {@link #getExtensionSubType() extensionSubType} passed to the constructor.
     * Called if, and only if this factory's {@link #getExtensionType() extensionType} is assignable from the given {@code extensionType}.
     * <p>The default implementation returns a new instance of {@code extensionSubType}, either
     * created from its public no-arg constructor or its public 1-arg constructor which can take the given {@code object}. Clients may subclass and
     * override this method in order to implement a more sophisticated instance creation.
     *
     * @param object        The object to be extended.
     * @param extensionType The type of the requested extension.
     * @return The extension object.
     * @throws Throwable If an error occurs.
     */
    protected E getExtensionImpl(T object, Class<E> extensionType) throws Throwable {
        Class<? extends E> subType = getExtensionSubType();
        try {
            Constructor<? extends E> constructor = subType.getConstructor(object.getClass());
            return constructor.newInstance(object);
        } catch (Exception e) {
            return subType.newInstance();
        }
    }

    /**
     * @return The array containing the {@link #getExtensionType() extensionType} and optionally
     *         the {@link #getExtensionSubType() extensionSubType} supported by this factory.
     */
    @Override
    public final Class<?>[] getExtensionTypes() {
        if (extensionType.equals(extensionSubType)) {
            return new Class<?>[]{getExtensionSubType()};
        }
        return new Class<?>[]{getExtensionSubType(), getExtensionType()};
    }

}
