package org.esa.snap.core.jexp;

/**
 * A term transformer is a term visitor that returns terms.
 *
 * @author Norman Fomferra
 */
public interface TermTransformer extends TermVisitor<Term> {
    /**
     * Applies the transformer to the given term and returns the transformed term.
     *
     * @param term The term to be transformed.
     * @return The transformed term.
     */
    default Term apply(Term term) {
        return term.accept(this);
    }
}
