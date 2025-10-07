package com.example.stocks.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {

    @Value("${app.csv.root:.}")
    private String csvRootFromProps;

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String appPort = dotenv.get("APP_PORT");
        String csvRoot = dotenv.get("CSV_ROOT");
        String exportDir = dotenv.get("EXPORT_DIR");
        String pdfTitle = dotenv.get("PDF_TITLE");

        if (appPort != null) System.setProperty("APP_PORT", appPort);
        if (csvRoot != null) System.setProperty("CSV_ROOT", csvRoot);
        if (exportDir != null) System.setProperty("EXPORT_DIR", exportDir);
        if (pdfTitle != null) System.setProperty("PDF_TITLE", pdfTitle);
    }
}


