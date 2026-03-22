package com.example.transport1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfGenerator {

    private static final String PDF_FILE_NAME = "Transport_Report.pdf";

    public static void generatePdf(Context context, List<TransportData> dataList) {

        if (dataList == null || dataList.isEmpty()) {
            Toast.makeText(context, "No data to generate PDF", Toast.LENGTH_SHORT).show();
            return;
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

                // -------------------- General Info --------------------
                paint.setColor(Color.BLACK);
                paint.setTextSize(14);
                paint.setFakeBoldText(true);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("GENERAL INFO", pageWidth / 2, y, paint);
                y += lineHeight;

                paint.setFakeBoldText(false);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawRect(margin - 5, y - lineHeight, pageWidth - margin + 5, y + 4 * lineHeight, getPaintStroke(Color.LTGRAY));
                canvas.drawText("Vehicle: " + data.vehicle, margin, y, paint);
                y += lineHeight;
                canvas.drawText("Factory: " + data.factory, margin, y, paint);
                y += lineHeight;
                canvas.drawText("Date: " + data.date, margin, y, paint);
                y += lineHeight;
                canvas.drawText("Weight: " + data.weight + " " + data.measurement, margin, y, paint);
                y += lineHeight + 10;

                // -------------------- Buy & Sell Info --------------------
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                canvas.drawText("BUY & SELL DETAILS", pageWidth / 2, y, paint);
                y += lineHeight;
                paint.setFakeBoldText(false);

                int halfWidth = (pageWidth - 2 * margin) / 2;
                int leftX = margin;
                int rightX = margin + halfWidth + 10;
                int sectionHeight = 5 * lineHeight + 10;

                // Draw boxes
                canvas.drawRect(leftX - 5, y - lineHeight, leftX + halfWidth, y + sectionHeight, getPaintStroke(Color.LTGRAY));
                canvas.drawRect(rightX - 5, y - lineHeight, rightX + halfWidth, y + sectionHeight, getPaintStroke(Color.LTGRAY));

                // Left: Buy
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("BUY INFO", leftX, y, paint);
                y += lineHeight;
                canvas.drawText("Weight: " + data.buyWeight, leftX, y, paint);
                y += lineHeight;
                canvas.drawText("Price: " + data.buyPrice, leftX, y, paint);
                y += lineHeight;
                canvas.drawText("GST: " + data.buyGST, leftX, y, paint);
                y += lineHeight;
                canvas.drawText("Total: " + data.buyTotalAmount, leftX, y, paint);

                // Right: Sell
                y -= 4 * lineHeight; // reset y to box top
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("SELL INFO", rightX, y, paint);
                y += lineHeight;
                canvas.drawText("Person: " + data.sellPerson, rightX, y, paint);
                y += lineHeight;
                canvas.drawText("Weight: " + data.sellWeight, rightX, y, paint);
                y += lineHeight;
                canvas.drawText("Price: " + data.sellPrice, rightX, y, paint);
                y += lineHeight;
                canvas.drawText("GST: " + data.sellGST, rightX, y, paint);
                y += lineHeight;
                canvas.drawText("Total: " + data.sellTotalAmount, rightX, y, paint);

                y += lineHeight + 10;

                // -------------------- Page Break --------------------
                if (y > pageHeight - 100) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }
            }

            pdfDocument.finishPage(page);

            File file = new File(context.getExternalFilesDir(null), PDF_FILE_NAME);
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            Toast.makeText(context, "PDF saved:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Utility for drawing rectangle outline
    private static Paint getPaintStroke(int color) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(2);
        return p;
    }
}