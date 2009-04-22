package org.esa.beam.visat.actions.session;

/**
 * todo - REVIEW this (mp, rq - 21.04.2009)
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public interface SessionAccessorHolder {

    Session.SessionAccessor getSessionAccessor();

    void setSessionAccessor(Session.SessionAccessor session);
}
