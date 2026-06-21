package distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class WorkerServer {
    public static final String DEFAULT_WORKER_NAME = "worker";

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9100;
        String workerName = args.length > 1 ? args[1] : DEFAULT_WORKER_NAME;
        if (args.length > 2) {
            System.setProperty("java.rmi.server.hostname", args[2]);
        }

        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(workerName, new MatrixWorkerImpl(port));

        System.out.printf("Worker RMI aguardando na porta %d com nome '%s'%n", port, workerName);
        if (args.length > 2) {
            System.out.println("Host RMI anunciado: " + args[2]);
        }
        Thread.currentThread().join();
    }
}
