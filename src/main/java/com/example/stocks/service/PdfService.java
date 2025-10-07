package com.example.stocks.service;

import com.example.stocks.model.StockPrice;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {
    private final ChartService chartService;
    private final String exportDir;
    private final String pdfTitle;

    public PdfService(ChartService chartService,
                      @Value("${app.export.dir:./exports}") String exportDir,
                      @Value("${app.pdf.title:Stocks Dashboard}") String pdfTitle) {
        this.chartService = chartService;
        this.exportDir = exportDir;
        this.pdfTitle = pdfTitle;
    }

    public byte[] generatePdf(String title,
                               Map<String, Object> chartData,
                               List<StockPrice> tableData,
                               Map<String, String> activeFilters) throws IOException, DocumentException {

        Files.createDirectories(Paths.get(exportDir));
        byte[] chartPng = chartService.renderLineChartPng(title, chartData);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
        doc.open();

        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph header = new Paragraph(pdfTitle, h1);
        header.setAlignment(Element.ALIGN_LEFT);
        doc.add(header);

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        doc.add(new Paragraph("Generated at: " + generatedAt, small));

        if (activeFilters != null && !activeFilters.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            activeFilters.forEach((k, v) -> sb.append(k).append(": ").append(v).append("  "));
            doc.add(new Paragraph("Filters: " + sb, small));
        }

        Image chart = Image.getInstance(chartPng);
        chart.scaleToFit(770, 300);
        chart.setAlignment(Element.ALIGN_CENTER);
        doc.add(chart);

        Table table = new Table(7);
        table.setWidth(100);
        table.setPadding(3);
        table.addCell(new Phrase("date"));
        table.addCell(new Phrase("ticker"));
        table.addCell(new Phrase("open"));
        table.addCell(new Phrase("high"));
        table.addCell(new Phrase("low"));
        table.addCell(new Phrase("close"));
        table.addCell(new Phrase("volume"));

        for (StockPrice sp : tableData) {
            table.addCell(sp.getDate() == null ? "" : sp.getDate().toString());
            table.addCell(sp.getTicker() == null ? "" : sp.getTicker());
            table.addCell(sp.getOpen() == null ? "" : sp.getOpen().toPlainString());
            table.addCell(sp.getHigh() == null ? "" : sp.getHigh().toPlainString());
            table.addCell(sp.getLow() == null ? "" : sp.getLow().toPlainString());
            table.addCell(sp.getClose() == null ? "" : sp.getClose().toPlainString());
            table.addCell(Long.toString(sp.getVolume()));
        }
        doc.add(table);

        doc.close();
        byte[] pdf = baos.toByteArray();

        String fileName = "stocks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        Path out = Paths.get(exportDir, fileName);
        Files.write(out, pdf);
        return pdf;
    }
}


