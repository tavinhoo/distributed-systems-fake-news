package distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MatrixWorkerRemote extends Remote {
    WorkerResult computeRange(WorkerTask task) throws RemoteException;
}
