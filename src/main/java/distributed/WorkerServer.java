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

        log("iniciando WorkerServer | porta=%d | nome=%s", port, workerName);
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(workerName, new MatrixWorkerImpl(workerName, port));

        log("MatrixWorker publicado no registry RMI | porta=%d | nome=%s", port, workerName);
        if (args.length > 2) {
            log("host RMI anunciado=%s", args[2]);
        }
        Thread.currentThread().join();
    }

    private static void log(String format, Object... args) {
        System.out.printf("[%tT.%<tL] [WorkerServer] %s%n",
                System.currentTimeMillis(), String.format(format, args));
    }
}
