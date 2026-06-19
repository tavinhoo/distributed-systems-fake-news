package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public class CsvWriter {
    private CsvWriter() {
    }

    public static void writeBenchmarkHeader(String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("version,total_ms,speedup,efficiency,threads_or_workers,ignorant,spreader,inactive,grok,neutralized_by_grok");
        }
    }

    public static void appendBenchmarkLine(String fileName,
                                           String version,
                                           double totalMillis,
                                           double speedup,
                                           double efficiency,
                                           int threadsOrWorkers,
                                           int ignorant,
                                           int spreader,
                                           int inactive,
                                           int grok,
                                           int neutralizedByGrok) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.printf(Locale.US, "%s,%.3f,%.4f,%.4f,%d,%d,%d,%d,%d,%d%n",
                    version, totalMillis, speedup, efficiency, threadsOrWorkers,
                    ignorant, spreader, inactive, grok, neutralizedByGrok);
        }
    }
}
