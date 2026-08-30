package com.example.transport1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.widget.Toast;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.util.List;

public class PdfGenerator {

    public static final String REPORT_BUY = "BUY";
    public static final String REPORT_SELL = "SELL";
    public static final String REPORT_BOTH = "BOTH";

    public static void generatePdf(Context context, List<TransportData> dataList, String reportType) {

        if (dataList == null || dataList.isEmpty()) {
            Toast.makeText(context, "No data to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName;
        switch (reportType) {
            case REPORT_BUY:
                fileName = "Buy_Report.pdf";
                break;
            case REPORT_SELL:
                fileName = "Sell_Report.pdf";
                break;
            case REPORT_BOTH:
            default:
                fileName = "Both_Report.pdf";
                break;
        }

        try {
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();

            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 40;
            int lineHeight = 20;
            int y = margin;
            int pageNumber = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            for (TransportData data : dataList) {

                // Check if we need a new page for the next record
                int recordHeight = 0;
                if (reportType.equals(REPORT_BOTH)) {
                    recordHeight = 18 * lineHeight; // Approximate height for both sections
                } else {
                    recordHeight = 10 * lineHeight; // Approximate height for single section
                }

                if (y + recordHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }

                // --- BUY DETAILS ---
                if (reportType.equals(REPORT_BUY) || reportType.equals(REPORT_BOTH)) {
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(14);
                    paint.setFakeBoldText(true);
                    paint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("BUY DETAILS", margin, y, paint);
                    y += lineHeight + 5;

                    paint.setFakeBoldText(false);
                    paint.setTextSize(12);
                    canvas.drawText("Vehicle: " + data.vehicle, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Date: " + data.date, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Factory: " + data.factory, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Weight: " + data.buyWeight + " " + data.measurement, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Price: " + data.buyPrice, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("GST: " + data.buyGST, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Total: " + data.buyTotalAmount, margin, y, paint);
                    y += lineHeight + 15;
                }

                // --- SELL DETAILS ---
                if (reportType.equals(REPORT_SELL) || reportType.equals(REPORT_BOTH)) {
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(14);
                    paint.setFakeBoldText(true);
                    paint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("SELL DETAILS", margin, y, paint);
                    y += lineHeight + 5;

                    paint.setFakeBoldText(false);
                    paint.setTextSize(12);
                    canvas.drawText("Vehicle: " + data.vehicle, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Date: " + data.date, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Person: " + data.sellPerson, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Weight: " + data.sellWeight + " " + data.measurement, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Price: " + data.sellPrice, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("GST: " + data.sellGST, margin, y, paint);
                    y += lineHeight;
                    canvas.drawText("Total: " + data.sellTotalAmount, margin, y, paint);
                    y += lineHeight + 20;
                }

                // Separator line between records
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(margin, y - 5, pageWidth - margin, y - 5, paint);
                y += 10;
            }

            pdfDocument.finishPage(page);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Transport/");

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream);
                    }
                    Toast.makeText(context, fileName + " saved in Downloads/Transport", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(context, "PDF save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "Unable to create PDF file", Toast.LENGTH_LONG).show();
            }

            pdfDocument.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}