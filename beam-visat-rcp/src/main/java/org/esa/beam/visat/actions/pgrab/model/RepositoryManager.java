package org.esa.beam.visat.actions.pgrab.model;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.actions.pgrab.ProductGrabberAction;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.DataProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.FileNameProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.ProductSizeProvider;
import org.esa.beam.visat.actions.pgrab.ui.ProductGrabber;
import org.esa.beam.visat.actions.pgrab.util.Callback;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 05.09.2005
 * Time: 10:48:14
 */

/**
 * The class RepositoryManager handels a list of repositories and a list of data provider.
 *
 * @author Marco Peters
 */
public class RepositoryManager {

    private List<Repository> repositoryList;
    private List<DataProvider> dataProviderList;
    private List<RepositoryManagerListener> listenerList;
    private ProgressMonitor pm;

    /**
     * Creates an instance of <code>RepositoryManager</code>.
     */
    public RepositoryManager() {
        repositoryList = new ArrayList<Repository>(10);
        dataProviderList = new ArrayList<DataProvider>(4);
        listenerList = new ArrayList<RepositoryManagerListener>(4);
        addDataProvider(new FileNameProvider());
        addDataProvider(new ProductSizeProvider());
    }

    /**
     * Adds a new <code>Repository<code> to the internal list of repositories.
     *
     * @param repository the repository to be added.
     */
    public void addRepository(final Repository repository) {
        if (!repositoryList.contains(repository)) {
            final DataProvider[] providers = dataProviderList.toArray(new DataProvider[dataProviderList.size()]);
            repository.setDataProviders(providers);
            repositoryList.add(repository);
            fireRepositoryAdded(repository);
        }
    }

    /**
     * Removes the given repository from the internal list of repositories.
     *
     * @param repository the repository to be removed.
     */
    public void removeRepository(final Repository repository) {
        if (repositoryList.remove(repository)) {
            fireRepositoryRemoved(repository);
        }
    }

    /**
     * Returns the <code>Repository</code> at the given index.
     *
     * @param index the index of the repository to return.
     *
     * @return the repository at the given index.
     */
    public Repository getRepository(final int index) {
        return repositoryList.get(index);
    }

    /**
     * Returns the <code>Repository</code> with the given <code>baseDir</code>.
     *
     * @param baseDir the <code>baseDir</code> of the repository to return.
     *
     * @return the repository with the given <code>baseDir</code>, or <code>null</code> if not found.
     */
    public Repository getRepository(final String baseDir) {
        for (final Repository repository : repositoryList) {
            if (repository.getBaseDir().getPath().equals(baseDir)) {
                return repository;
            }
        }
        return null;
    }

    /**
     * Retrieves the number of repositories.
     *
     * @return the number of repositories.
     */
    public int getNumRepositories() {
        return repositoryList.size();
    }

    /**
     * Returns an array of registered {@link Repository}.
     *
     * @return an array of repositories, never null
     */
    public Repository[] getRepositories() {
        return repositoryList.toArray(new Repository[repositoryList.size()]);
    }


    /**
     * Adds a <code>DataProvider<code>.
     *
     * @param dataProvider the <code>DataProvider</code> to be added.
     */
    public void addDataProvider(final DataProvider dataProvider) {
        if (!dataProviderList.contains(dataProvider)) {
            dataProviderList.add(dataProvider);
        }
    }

    /**
     * Returns the <code>DataProvider</code> at the given index.
     *
     * @param index the index of the <code>DataProvider</code> to return.
     *
     * @return the <code>DataProvider</code> at the given index.
     */
    public DataProvider getDataProvider(final int index) {
        return dataProviderList.get(index);
    }

    /**
     * Retrieves the number of <code>DataProvider</code>.
     *
     * @return the number of <code>DataProvider</code>.
     */
    public int getNumDataProviders() {
        return dataProviderList.size();
    }

    /**
     * Returns an array of registered {@link DataProvider}.
     *
     * @return an array <code>DataProvider</code>, never null
     */
    public DataProvider[] getDataProviders() {
        return dataProviderList.toArray(new DataProvider[dataProviderList.size()]);
    }

    /**
     * Adds a <code>RepositoryManagerListener</code>.
     *
     * @param listener the <code>RepositoryManagerListener</code> to be added.
     */
    public void addListener(final RepositoryManagerListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a <code>RepositoryManagerListener</code>.
     *
     * @param listener the <code>RepositoryManagerListener</code> to be removed.
     */
    public void removeListener(final RepositoryManagerListener listener) {
        listenerList.remove(listener);
    }

    /**
     * This method starts an seperate thread. Each {@link RepositoryEntry} of the given <code>Repository</code> is
     * filled with data. A possibly running update process is stopped before the new one is started.
     *
     * @param repository the <code>Repository</code> whose {@link RepositoryEntry entries} are updated.
     * @param pm         a {@link ProgressMonitor} to get informed about the progress.
     * @param uiCallback a callback to handle ui updates.
     */
    public void startUpdateRepository(final Repository repository, final ProgressMonitor pm,
                                      final Callback uiCallback) {
        stopUpdateRepository();

        this.pm = pm;

        final UpdateProcess updateProcess = new UpdateProcess(repository, pm, uiCallback);
        updateProcess.execute();
    }

    /**
     * This method stops the process started by a call to
     * {@link #startUpdateRepository(Repository, ProgressMonitor, Callback)}  startUpdateRepository()}.
     * If no process is started the method has no effect.
     */
    public void stopUpdateRepository() {
        if (pm != null) {
            pm.setCanceled(true);
            pm = null;
        }
    }


    private void fireRepositoryAdded(final Repository repository) {
        for (final RepositoryManagerListener listener : listenerList) {
            listener.repositoryAdded(repository);
        }
    }

    private void fireRepositoryRemoved(final Repository repsoitory) {
        for (final RepositoryManagerListener listener : listenerList) {
            listener.repositoryRemoved(repsoitory);
        }
    }

    private class UpdateProcess extends SwingWorker<List<ExceptionObject>, Object> {

        private Repository repository;
        private Runnable uiRunnable;
        private ProgressMonitor pm;

        public UpdateProcess(final Repository repository, final ProgressMonitor pm, final Callback uiCallback) {
            this.pm = pm;
            this.repository = repository;
            uiRunnable = new Runnable() {
                public void run() {
                    uiCallback.callback();
                }
            };
        }

        @Override
        protected List<ExceptionObject> doInBackground() throws Exception {
            final int maxProvider = dataProviderList.size() - 1;
            final ArrayList<RepositoryEntry> entriesToRemove = new ArrayList<RepositoryEntry>(10);
            final List<ExceptionObject> exceptionList = new ArrayList<ExceptionObject>(5);

            RepositoryScanner.collectEntries(repository);
            SwingUtilities.invokeLater(uiRunnable);
            pm.beginTask("Updating repository...", 1 + (repository.getEntryCount() * dataProviderList.size()));
            try {
                final int maxRepEntries = repository.getEntryCount();
                final Object[] messageArgs = new Object[]{null, maxRepEntries, null};

                if (pm.isCanceled()) {
                    return exceptionList;
                }

                // display file name and file size first
                for (int i = 0; i < maxRepEntries; i++) {
                    final RepositoryEntry entry = repository.getEntry(i);
                    for (int j = 0; j <= 1; j++) {
                        final DataProvider dataProvider = dataProviderList.get(j);
                        try {
                            final Object data = dataProvider.getData(entry, repository);
                            entry.setData(dataProvider.getTableColumn().getModelIndex(), data);
                        } catch (IOException e) {
                            exceptionList.add(new ExceptionObject(entry.getProductFile().getName(), e));
                        }
                    }
                }
                pm.worked(1);

                for (int i = 0; i < maxRepEntries; i++) {
                    messageArgs[0] = i + 1;
                    if (pm.isCanceled()) {
                        return exceptionList;
                    }
                    final RepositoryEntry entry = repository.getEntry(i);
                    try {
                        if (!entry.getProductFile().exists()) {
                            // start at product properties, we already have name and size
                            for (int j = 2; j <= maxProvider; j++) {
                                if (pm.isCanceled()) {
                                    return exceptionList;
                                }
                                final DataProvider dataProvider = dataProviderList.get(j);
                                messageArgs[2] = dataProvider.getTableColumn().getHeaderValue();
                                pm.setSubTaskName(
                                        MessageFormat.format("Synchronizing repository entry {0} of {1}: {2}...",
                                                             messageArgs));
                                dataProvider.cleanUp(entry, repository);
                                entriesToRemove.add(entry);
                                pm.worked(1);
                            }
                        } else {
                            try {
                                // start at product properties, we already have name and size
                                for (int j = 2; j <= maxProvider; j++) {
                                    if (pm.isCanceled()) {
                                        return exceptionList;
                                    }
                                    final DataProvider dataProvider = dataProviderList.get(j);
                                    messageArgs[2] = dataProvider.getTableColumn().getHeaderValue();
                                    pm.setSubTaskName(
                                            MessageFormat.format("Updating repository entry {0} of {1}: {2}...",
                                                                 messageArgs)); /*I18N*/
                                    if (dataProvider.mustCreateData(entry, repository)) {
                                        if (entry.getProduct() == null) {
                                            entry.openProduct();
                                            if (entry.getProduct() == null) {
                                                pm.worked(maxProvider - j);
                                                break;
                                            }
                                        }

                                        dataProvider.createData(entry, repository);
                                    }
                                    final Object data = dataProvider.getData(entry, repository);
                                    entry.setData(dataProvider.getTableColumn().getModelIndex(), data);
                                    pm.worked(1);
                                }
                            } catch (Exception e) {
                                exceptionList.add(new ExceptionObject(entry.getProductFile().getName(), e));
                            }
                        }
                    } finally {
                        entry.closeProduct();
                        repository.savePropertyMap();
                        SwingUtilities.invokeLater(uiRunnable);
                    }
                }
            } finally {
                for (RepositoryEntry entry : entriesToRemove) {
                    repository.removeEntry(entry);
                }
                pm.done();
                SwingUtilities.invokeLater(uiRunnable);
            }
            return exceptionList;
        }

        @Override
        public void done() {
            List<ExceptionObject> exceptionList;
            try {
                exceptionList = get();
            } catch (Exception e) {
                exceptionList = new ArrayList<ExceptionObject>(1);
                exceptionList.add(new ExceptionObject("", e));
            }
            if (exceptionList != null && exceptionList.size() != 0) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not able to read the following file(s):\n\n");
                for (final ExceptionObject exception : exceptionList) {
                    Debug.trace(exception.throwable);
                    stringBuilder.append(exception.productFileName);
                    stringBuilder.append("\n");
                }
                final String message = stringBuilder.toString();
                ProductGrabber productGrabber = ProductGrabberAction.getInstance().getProductGrabber();
                JOptionPane.showMessageDialog(productGrabber.getFrame(), message, "I/O Errors", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class ExceptionObject {

        Throwable throwable;
        String productFileName;

        public ExceptionObject(final String productFileName, final Throwable throwable) {
            this.productFileName = productFileName;
            this.throwable = throwable;
        }
    }
}
