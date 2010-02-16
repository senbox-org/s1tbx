package org.esa.beam.visat.actions.pgrab.model;



/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 05.09.2005
 * Time: 11:06:16
 */

/**
 * Clients can implement <code>RepositoryManagerListener</code> to be informed
 * on changes of {@link RepositoryManager}.
 *
 * @author Marco Peters
 */
public interface RepositoryManagerListener {

    /**
     * Implementation should handle that a new <code>Repository<code> was added.
     *
     * @param repository the <code>Repository<code> that was added.
     */
    void repositoryAdded(Repository repository);

    /**
     * Implementation should handle that a new <code>Repository<code> was removed.
     *
     * @param repository the <code>Repository<code> that was removed.
     */
    void repositoryRemoved(Repository repository);

}
