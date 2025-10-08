package com.example.stocks.service;

import com.example.stocks.model.StockPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CsvStockLoader {

    private final String csvRoot;

    public CsvStockLoader(@Value("${app.csv.root:.}") String csvRoot) {
        this.csvRoot = csvRoot;
    }

    public List<StockPrice> loadAll() {
        Path root = Paths.get(csvRoot);
        if (!Files.exists(root)) {
            return Collections.emptyList();
        }

        List<Path> csvFiles;
        try (Stream<Path> stream = Files.list(root)) {
            csvFiles = stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".csv")).collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }

        List<StockPrice> all = new ArrayList<>();
        for (Path p : csvFiles) {
            all.addAll(loadFile(p));
        }
        return all;
    }

    private List<StockPrice> loadFile(Path path) {
        List<StockPrice> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;
            String[] headers = splitCsv(headerLine);
            Map<String, Integer> idx = mapHeaderIndexes(headers);
            String line;
            DateTimeFormatter[] fmts = new DateTimeFormatter[]{
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("yyyyMMdd"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            };
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsv(line);
                try {
                    StockPrice sp = new StockPrice();
                    sp.setDate(parseDate(get(cols, idx, "date"), fmts));
                    sp.setTicker(safe(get(cols, idx, "ticker")));
                    sp.setOpen(parseDec(get(cols, idx, "open")));
                    sp.setHigh(parseDec(get(cols, idx, "high")));
                    sp.setLow(parseDec(get(cols, idx, "low")));
                    sp.setClose(parseDec(get(cols, idx, "close")));
                    sp.setVolume(parseLong(get(cols, idx, "volume")));
                    rows.add(sp);
                } catch (Exception ignore) {
                    // skip bad row
                }
            }
        } catch (IOException ignored) {
        }
        return rows;
    }

    private Map<String, Integer> mapHeaderIndexes(String[] headers) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String raw = headers[i].trim().toLowerCase(Locale.ROOT);
            // normalize known aliases to the expected canonical names
            String h = switch (raw) {
                case "portdate" -> "date";
                case "portid" -> "ticker";
                case "opening" -> "open";
                case "closing" -> "close";
                default -> raw;
            };
            m.put(h, i);
        }
        return m;
    }

    private String[] splitCsv(String line) {
        // simple split, assumes no embedded commas in quotes for provided data
        return line.split(",");
    }

    private String get(String[] cols, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i < 0 || i >= cols.length) return null;
        return cols[i];
    }

    private String safe(String s) {
        return s == null ? null : s.trim();
    }

    private LocalDate parseDate(String s, DateTimeFormatter[] fmts) {
        if (s == null) return null;
        String v = s.trim();
        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(v, f); } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseDec(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(s.trim());
    }

    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }
}


