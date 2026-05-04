package com.project.report;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;

public class PDFReport {

    public static void generateReport() {
        try {
            String basePath = System.getProperty("user.dir");

            String pdfPath = basePath + "\\report.pdf";
            String logPath = basePath + "\\logs\\log.txt";

            PdfWriter writer = new PdfWriter(pdfPath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 🔥 Title
            document.add(new Paragraph("AI Exam Proctoring Report")
                    .setBold().setFontSize(18));

            document.add(new Paragraph("Generated at: " + LocalDateTime.now()));
            document.add(new Paragraph("--------------------------------------------------"));

            // 🔥 Read logs
            BufferedReader reader = new BufferedReader(new FileReader(logPath));
            String line;

            while ((line = reader.readLine()) != null) {
                document.add(new Paragraph(line));
            }

            reader.close();
            document.close();

            System.out.println("✅ PDF Generated: " + pdfPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}