package distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class WorkerServer {
    public static final String DEFAULT_WORKER_NAME = "worker";

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9100;
        String workerName = args.length > 1 ? args[1] : DEFAULT_WORKER_NAME;

        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(workerName, new MatrixWorkerImpl());

        System.out.printf("Worker RMI aguardando na porta %d com nome '%s'%n", port, workerName);
        Thread.currentThread().join();
    }
}
