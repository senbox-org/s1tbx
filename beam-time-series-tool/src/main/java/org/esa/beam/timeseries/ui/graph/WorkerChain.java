package org.esa.beam.timeseries.ui.graph;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WorkerChain {

    private final List<SwingWorker> synchronizedWorkerChain;
    private SwingWorker unchainedWorker;
    private boolean workerIsRunning = false;

    WorkerChain() {
        synchronizedWorkerChain = Collections.synchronizedList(new ArrayList<SwingWorker>());
    }

    synchronized void setOrExecuteNextWorker(SwingWorker w, boolean chained) {
        if (w == null) {
            return;
        }
        if (workerIsRunning) {
            if (chained) {
                synchronizedWorkerChain.add(w);
            } else {
                unchainedWorker = w;
            }
        } else {
            if (chained) {
                synchronizedWorkerChain.add(w);
                executeFirstWorkerInChain();
            } else {
                unchainedWorker = w;
                w.execute();
            }
            workerIsRunning = true;
        }
    }

    synchronized void removeCurrentWorkerAndExecuteNext(SwingWorker currentWorker) {
        synchronizedWorkerChain.remove(currentWorker);
        if (unchainedWorker == currentWorker) {
            unchainedWorker = null;
        }
        if (synchronizedWorkerChain.size() > 0) {
            executeFirstWorkerInChain();
            return;
        }
        if (unchainedWorker != null) {
            unchainedWorker.execute();
            return;
        }
        workerIsRunning = false;
    }

    private void executeFirstWorkerInChain() {
        synchronizedWorkerChain.get(0).execute();
    }
}
