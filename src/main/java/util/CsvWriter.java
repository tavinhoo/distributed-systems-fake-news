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
            writer.println("scenario,version,rows,columns,generations,initial_spreader_rate,units,total_ms,speedup,efficiency,ignorant,spreader,inactive,grok,whatsapp_group,influencer,journalist,neutralized_by_grok");
        }
    }

    public static void appendBenchmarkLine(String fileName,
                                           String scenario,
                                           String version,
                                           int rows,
                                           int columns,
                                           int generations,
                                           double initialSpreaderRate,
                                           int units,
                                           double totalMillis,
                                           double speedup,
                                           double efficiency,
                                           int ignorant,
                                           int spreader,
                                           int inactive,
                                           int grok,
                                           int whatsAppGroup,
                                           int influencer,
                                           int journalist,
                                           int neutralizedByGrok) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.printf(Locale.US, "%s,%s,%d,%d,%d,%.4f,%d,%.3f,%.4f,%.4f,%d,%d,%d,%d,%d,%d,%d,%d%n",
                    scenario, version, rows, columns, generations, initialSpreaderRate,
                    units, totalMillis, speedup, efficiency,
                    ignorant, spreader, inactive, grok, whatsAppGroup, influencer, journalist, neutralizedByGrok);
        }
    }
}
