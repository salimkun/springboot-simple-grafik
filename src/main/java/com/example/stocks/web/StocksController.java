package com.example.stocks.web;

import com.example.stocks.model.StockPrice;
import com.example.stocks.service.ChartService;
import com.example.stocks.service.PdfService;
import com.example.stocks.service.StockQueryService;
import com.lowagie.text.DocumentException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks")
public class StocksController {

    private final StockQueryService queryService;
    private final ChartService chartService;
    private final PdfService pdfService;

    public StocksController(StockQueryService queryService, ChartService chartService, PdfService pdfService) {
        this.queryService = queryService;
        this.chartService = chartService;
        this.pdfService = pdfService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(name = "tickers", required = false) String tickers,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "sortBy", defaultValue = "date") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir
    ) {
        Set<String> tset = parseTickers(tickers);
        List<StockPrice> data = queryService.query(tset, startDate, endDate, sortBy, sortDir);
        if (data.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("message", "Tidak ada data untuk filter yang dipilih");
            empty.put("rows", 0);
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/chart")
    public ResponseEntity<?> chart(
            @RequestParam(name = "tickers", required = false) String tickers,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Set<String> tset = parseTickers(tickers);
        Map<String, Object> chartData = chartService.buildChartData(tset, startDate, endDate);
        if (((List<?>) chartData.getOrDefault("categories", List.of())).isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("message", "Tidak ada data untuk ditampilkan pada grafik");
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(name = "tickers", required = false) String tickers,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "sortBy", defaultValue = "date") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir
    ) throws IOException, DocumentException {
        Set<String> tset = parseTickers(tickers);
        List<StockPrice> data = queryService.query(tset, startDate, endDate, sortBy, sortDir);
        Map<String, Object> chartData = chartService.buildChartData(tset, startDate, endDate);
        Map<String, String> filters = new HashMap<>();
        filters.put("tickers", tset == null || tset.isEmpty() ? "(all)" : String.join(",", tset));
        filters.put("startDate", startDate == null ? "" : startDate.toString());
        filters.put("endDate", endDate == null ? "" : endDate.toString());
        byte[] pdf = pdfService.generatePdf("Stocks Dashboard", chartData, data, filters);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dashboard.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private Set<String> parseTickers(String tickers) {
        if (tickers == null || tickers.isBlank()) return new HashSet<>();
        return java.util.Arrays.stream(tickers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }
}


