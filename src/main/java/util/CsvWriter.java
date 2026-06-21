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
            writer.println("scenario,version,status,rows,columns,generations,initial_spreader_rate,units,total_ms,speedup,efficiency,final_grid_match,ignorant,spreader,inactive,grok,bot,influencer,echo_chamber,fact_checker,journalist,neutralized_by_grok,error");
        }
    }

    public static void appendBenchmarkLine(String fileName,
                                           String scenario,
                                           String version,
                                           String status,
                                           int rows,
                                           int columns,
                                           int generations,
                                           double initialSpreaderRate,
                                           int units,
                                           double totalMillis,
                                           double speedup,
                                           double efficiency,
                                           String finalGridMatch,
                                           int ignorant,
                                           int spreader,
                                           int inactive,
                                           int grok,
                                           int bot,
                                           int influencer,
                                           int echoChamber,
                                           int factChecker,
                                           int journalist,
                                           int neutralizedByGrok,
                                           String error) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.printf(Locale.US, "%s,%s,%s,%d,%d,%d,%.4f,%d,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s%n",
                    csv(scenario), csv(version), csv(status), rows, columns, generations, initialSpreaderRate,
                    units, format(totalMillis), format(speedup), format(efficiency), csv(finalGridMatch),
                    ignorant, spreader, inactive, grok, bot, influencer,
                    echoChamber, factChecker, journalist, neutralizedByGrok, csv(error));
        }
    }

    private static String format(double value) {
        if (Double.isNaN(value)) {
            return "";
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private static String csv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
