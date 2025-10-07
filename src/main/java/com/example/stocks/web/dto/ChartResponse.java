package com.example.stocks.web.dto;

import java.util.List;
import java.util.Map;

public class ChartResponse {
    private List<String> categories; // dates as ISO strings
    private Map<String, List<Double>> series; // ticker -> close values

    public ChartResponse() {}

    public ChartResponse(List<String> categories, Map<String, List<Double>> series) {
        this.categories = categories;
        this.series = series;
    }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public Map<String, List<Double>> getSeries() { return series; }
    public void setSeries(Map<String, List<Double>> series) { this.series = series; }
}


