package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;

public abstract class AbstractSingleTypeDomConverter<T> implements DomConverter, SessionAccessorHolder {

    private final Class<? extends T> type;
    private transient Session.SessionAccessor sessionAccessor;

    protected AbstractSingleTypeDomConverter(Class<? extends T> type) {
        this.type = type;
    }

    @Override
    public final Class<? extends T> getValueType() {
        return type;
    }

    @Override
    public abstract T convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                       ValidationException;

    @Override
    public abstract void convertValueToDom(Object value, DomElement parentElement);

    @Override
    public final Session.SessionAccessor getSessionAccessor() {
        return sessionAccessor;
    }

    @Override
    public final void setSessionAccessor(Session.SessionAccessor sessionAccessor) {
        this.sessionAccessor = sessionAccessor;
    }
}
