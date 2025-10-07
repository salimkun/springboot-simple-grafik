package com.example.stocks.service;

import com.example.stocks.model.StockPrice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockQueryService {

    private final CsvStockLoader loader;

    public StockQueryService(CsvStockLoader loader) {
        this.loader = loader;
    }

    public List<StockPrice> query(Set<String> tickers,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  String sortBy,
                                  String sortDir) {
        List<StockPrice> all = loader.loadAll();
        if (all.isEmpty()) return all;

        List<StockPrice> filtered = all.stream()
                .filter(sp -> sp.getDate() != null && sp.getTicker() != null)
                .filter(sp -> tickers == null || tickers.isEmpty() || tickers.contains(sp.getTicker()))
                .filter(sp -> startDate == null || !sp.getDate().isBefore(startDate))
                .filter(sp -> endDate == null || !sp.getDate().isAfter(endDate))
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<StockPrice> comparator = buildComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        filtered.sort(comparator);
        return filtered;
    }

    private Comparator<StockPrice> buildComparator(String sortBy) {
        String key = sortBy == null ? "date" : sortBy.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ticker" -> Comparator.comparing(StockPrice::getTicker, Comparator.nullsLast(String::compareTo));
            case "open" -> Comparator.comparing(sp -> valueOrNull(sp.getOpen()));
            case "high" -> Comparator.comparing(sp -> valueOrNull(sp.getHigh()));
            case "low" -> Comparator.comparing(sp -> valueOrNull(sp.getLow()));
            case "close" -> Comparator.comparing(sp -> valueOrNull(sp.getClose()));
            case "volume" -> Comparator.comparing(StockPrice::getVolume);
            default -> Comparator.comparing(StockPrice::getDate, Comparator.nullsLast(LocalDate::compareTo));
        };
    }

    private Double valueOrNull(java.math.BigDecimal d) {
        return d == null ? null : d.doubleValue();
    }
}


