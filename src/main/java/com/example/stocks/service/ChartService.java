package com.example.stocks.service;

import com.example.stocks.model.StockPrice;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChartService {
    private final StockQueryService queryService;

    public ChartService(StockQueryService queryService) {
        this.queryService = queryService;
    }

    public Map<String, Object> buildChartData(Set<String> tickers,
                                              java.time.LocalDate start,
                                              java.time.LocalDate end) {
        List<StockPrice> data = queryService.query(tickers, start, end, "date", "asc");
        Map<String, List<StockPrice>> byTicker = data.stream().collect(Collectors.groupingBy(StockPrice::getTicker));
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<String> categories = data.stream()
                .map(sp -> sp.getDate().format(fmt))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<Double>> series = new LinkedHashMap<>();
        for (String t : byTicker.keySet().stream().sorted().collect(Collectors.toList())) {
            List<Double> closes = new ArrayList<>();
            Map<String, Double> dateToClose = byTicker.get(t).stream()
                    .sorted(Comparator.comparing(StockPrice::getDate))
                    .collect(Collectors.toMap(sp -> sp.getDate().format(fmt), sp -> sp.getClose() == null ? null : sp.getClose().doubleValue(), (a, b) -> b, LinkedHashMap::new));
            for (String c : categories) {
                closes.add(dateToClose.getOrDefault(c, null));
            }
            series.put(t, closes);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", categories);
        response.put("series", series);
        return response;
    }

    public byte[] renderLineChartPng(String title,
                                     Map<String, Object> chartData) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) chartData.get("categories");
        @SuppressWarnings("unchecked")
        Map<String, List<Double>> series = (Map<String, List<Double>>) chartData.get("series");

        XYChart chart = new XYChartBuilder().width(900).height(450).title(title).xAxisTitle("Date").yAxisTitle("Close").build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setChartTitleVisible(true);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.getStyler().setPlotContentSize(.95);
        chart.getStyler().setMarkerSize(4);

        double[] x = new double[categories.size()];
        for (int i = 0; i < categories.size(); i++) x[i] = i;

        for (Map.Entry<String, List<Double>> e : series.entrySet()) {
            List<Double> yList = e.getValue();
            double[] y = new double[yList.size()];
            for (int i = 0; i < yList.size(); i++) y[i] = yList.get(i) == null ? Double.NaN : yList.get(i);
            chart.addSeries(e.getKey(), x, y);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
        return baos.toByteArray();
    }
}


