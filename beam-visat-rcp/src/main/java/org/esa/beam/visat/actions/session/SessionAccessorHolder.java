package org.esa.beam.visat.actions.session;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public interface SessionAccessorHolder {

    Session.SessionAccessor getSessionAccessor();

    void setSessionAccessor(Session.SessionAccessor session);
}
