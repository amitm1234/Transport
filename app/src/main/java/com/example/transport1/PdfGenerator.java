package com.example.transport1;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 40;
            float left = margin;
            float right = pageWidth - margin;
            int pageNumber = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            float y = margin;

            // --- 1. TITLE & SUBTITLE (First Page Top) ---
            String titleText;
            switch (reportType) {
                case REPORT_BUY:
                    titleText = "Buy Report";
                    break;
                case REPORT_SELL:
                    titleText = "Sell Report";
                    break;
                case REPORT_BOTH:
                default:
                    titleText = "Both Report";
                    break;
            }

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.parseColor("#1A1A1A"));
            titlePaint.setTextSize(16);
            titlePaint.setFakeBoldText(true);
            titlePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(titleText, pageWidth / 2f, y + 16, titlePaint);
            y += 24;

            String dateText = "Date: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            Paint subtitlePaint = new Paint();
            subtitlePaint.setColor(Color.parseColor("#777777"));
            subtitlePaint.setTextSize(10);
            subtitlePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(dateText, pageWidth / 2f, y + 10, subtitlePaint);
            y += 25;

            // Shared paints for rows
            Paint labelPaint = new Paint();
            Paint valuePaint = new Paint();
            Paint headingPaint = new Paint();
            headingPaint.setColor(Color.parseColor("#1A1A1A"));
            headingPaint.setTextSize(11);
            headingPaint.setFakeBoldText(true);

            Paint cardStrokePaint = new Paint();
            cardStrokePaint.setStyle(Paint.Style.STROKE);
            cardStrokePaint.setColor(Color.parseColor("#CCCCCC"));
            cardStrokePaint.setStrokeWidth(1.5f);

            Paint dividerPaint = new Paint();
            dividerPaint.setColor(Color.parseColor("#DDDDDD"));
            dividerPaint.setStrokeWidth(1);

            double sumBuy = 0;
            double sumSell = 0;

            int cardHeight = REPORT_BOTH.equals(reportType) ? 162 : 146;

            // --- 2. RECORD CARDS ---
            for (TransportData data : dataList) {

                if (data != null) {
                    sumBuy += parseDoubleSafely(data.buyTotalAmount);
                    sumSell += parseDoubleSafely(data.sellTotalAmount);
                }

                // Check page break
                if (y + cardHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }

                float cardTop = y;

                // Draw Header Strip
                drawCardHeader(canvas, data.vehicle, data.date, left, cardTop, right, 26);
                float contentTop = cardTop + 26;
                float cardBottom;

                if (REPORT_BOTH.equals(reportType)) {
                    float midX = left + (right - left) / 2f;
                    float yCursor = contentTop + 18;

                    // Column Headings
                    headingPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("BUY", left + 12, yCursor, headingPaint);
                    canvas.drawText("SELL", midX + 12, yCursor, headingPaint);
                    yCursor += 18;

                    // Rows
                    drawRow(canvas, "Factory", data.factory, left + 12, midX - 12, yCursor, false, labelPaint, valuePaint);
                    drawRow(canvas, "Person", data.sellPerson, midX + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Weight", formatWeight(data.buyWeight, data.measurement), left + 12, midX - 12, yCursor, false, labelPaint, valuePaint);
                    drawRow(canvas, "Weight", formatWeight(data.sellWeight, data.measurement), midX + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Price", data.buyPrice, left + 12, midX - 12, yCursor, false, labelPaint, valuePaint);
                    drawRow(canvas, "Price", data.sellPrice, midX + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "GST", data.buyGST, left + 12, midX - 12, yCursor, false, labelPaint, valuePaint);
                    drawRow(canvas, "GST", data.sellGST, midX + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Total", data.buyTotalAmount, left + 12, midX - 12, yCursor, true, labelPaint, valuePaint);
                    drawRow(canvas, "Total", data.sellTotalAmount, midX + 12, right - 12, yCursor, true, labelPaint, valuePaint);
                    yCursor += 14;

                    // Middle vertical divider
                    canvas.drawLine(midX, contentTop + 6, yCursor - 4, midX, dividerPaint);

                    // Profit / Loss Bottom Strip
                    float stripTop = yCursor;
                    float stripHeight = 24;
                    float stripBottom = stripTop + stripHeight;

                    double profit = parseDoubleSafely(data.sellTotalAmount) - parseDoubleSafely(data.buyTotalAmount);
                    int pBgColor = profit >= 0 ? Color.parseColor("#EAF3DE") : Color.parseColor("#FCEBEB");
                    int pTextColor = profit >= 0 ? Color.parseColor("#3B6D11") : Color.parseColor("#791F1F");
                    String pText = profit >= 0 ? "Profit: +" + formatCurrency(profit) : "Loss: -" + formatCurrency(Math.abs(profit));

                    Paint pBgPaint = new Paint();
                    pBgPaint.setStyle(Paint.Style.FILL);
                    pBgPaint.setColor(pBgColor);
                    canvas.drawRoundRect(left, stripTop, right, stripBottom, 8, 8, pBgPaint);
                    canvas.drawRect(left, stripTop, right, stripTop + 8, pBgPaint);

                    Paint pDivider = new Paint();
                    pDivider.setColor(Color.parseColor("#CCCCCC"));
                    pDivider.setStrokeWidth(1);
                    canvas.drawLine(left, stripTop, right, stripTop, pDivider);

                    Paint pTextPaint = new Paint();
                    pTextPaint.setColor(pTextColor);
                    pTextPaint.setTextSize(10);
                    pTextPaint.setFakeBoldText(true);
                    pTextPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(pText, right - 12, stripTop + 16, pTextPaint);

                    cardBottom = stripBottom;

                } else if (REPORT_BUY.equals(reportType)) {
                    float yCursor = contentTop + 18;

                    headingPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("BUY", left + 12, yCursor, headingPaint);
                    yCursor += 18;

                    drawRow(canvas, "Factory", data.factory, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Weight", formatWeight(data.buyWeight, data.measurement), left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Price", data.buyPrice, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "GST", data.buyGST, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Total", data.buyTotalAmount, left + 12, right - 12, yCursor, true, labelPaint, valuePaint);
                    yCursor += 14;

                    cardBottom = yCursor;

                } else {
                    // REPORT_SELL
                    float yCursor = contentTop + 18;

                    headingPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("SELL", left + 12, yCursor, headingPaint);
                    yCursor += 18;

                    drawRow(canvas, "Person", data.sellPerson, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Weight", formatWeight(data.sellWeight, data.measurement), left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Price", data.sellPrice, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "GST", data.sellGST, left + 12, right - 12, yCursor, false, labelPaint, valuePaint);
                    yCursor += 16;

                    drawRow(canvas, "Total", data.sellTotalAmount, left + 12, right - 12, yCursor, true, labelPaint, valuePaint);
                    yCursor += 14;

                    cardBottom = yCursor;
                }

                // Draw outer rounded stroke for card
                canvas.drawRoundRect(left, cardTop, right, cardBottom, 8, 8, cardStrokePaint);

                y = cardBottom + 14;
            }

            // --- 3. FINAL SUMMARY STRIP ---
            int summaryHeight = 32;
            if (y + summaryHeight > pageHeight - margin) {
                pdfDocument.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            drawSummaryStrip(canvas, reportType, sumBuy, sumSell, left, y, right, summaryHeight);

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

    private static void drawCardHeader(Canvas canvas, String vehicle, String date, float left, float top, float right, float height) {
        Paint bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.parseColor("#F3F1EA"));

        canvas.drawRoundRect(left, top, right, top + height, 8, 8, bgPaint);
        canvas.drawRect(left, top + height - 8, right, top + height, bgPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(11);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.LEFT);

        String vStr = (vehicle != null && !vehicle.trim().isEmpty()) ? vehicle.trim() : "-";
        String dStr = (date != null && !date.trim().isEmpty()) ? date.trim() : "-";
        String headerText = "Vehicle: " + vStr + "  |  Date: " + dStr;

        canvas.drawText(headerText, left + 12, top + 17, textPaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E5E3DC"));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(left, top + height, right, top + height, linePaint);
    }

    private static void drawRow(Canvas canvas, String label, String value, float leftX, float rightX, float y, boolean isBold, Paint labelPaint, Paint valuePaint) {
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setColor(Color.parseColor("#888888"));
        labelPaint.setTextSize(10);
        labelPaint.setFakeBoldText(false);

        valuePaint.setTextAlign(Paint.Align.RIGHT);
        valuePaint.setColor(Color.parseColor("#1A1A1A"));
        valuePaint.setTextSize(10);
        valuePaint.setFakeBoldText(isBold);

        canvas.drawText(label, leftX, y, labelPaint);

        String valStr = (value != null && !value.trim().isEmpty()) ? value.trim() : "-";
        canvas.drawText(valStr, rightX, y, valuePaint);
    }

    private static void drawSummaryStrip(Canvas canvas, String reportType, double sumBuy, double sumSell, float left, float top, float right, float height) {
        int bgColor;
        int strokeColor;
        int textColor;
        String summaryText;

        if (REPORT_BUY.equals(reportType)) {
            bgColor = Color.parseColor("#E6F1FB");
            strokeColor = Color.parseColor("#B8D8F8");
            textColor = Color.parseColor("#0C447C");
            summaryText = "Total buy amount: " + formatCurrency(sumBuy);
        } else if (REPORT_SELL.equals(reportType)) {
            bgColor = Color.parseColor("#EAF3DE");
            strokeColor = Color.parseColor("#C2E0B4");
            textColor = Color.parseColor("#3B6D11");
            summaryText = "Total sell amount: " + formatCurrency(sumSell);
        } else {
            bgColor = Color.parseColor("#F3F0F8");
            strokeColor = Color.parseColor("#DDD6EC");
            double net = sumSell - sumBuy;
            if (net >= 0) {
                textColor = Color.parseColor("#3B6D11");
                summaryText = "Net profit: +" + formatCurrency(net);
            } else {
                textColor = Color.parseColor("#791F1F");
                summaryText = "Net loss: -" + formatCurrency(Math.abs(net));
            }
        }

        Paint bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(bgColor);
        canvas.drawRoundRect(left, top, right, top + height, 8, 8, bgPaint);

        Paint strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(left, top, right, top + height, 8, 8, strokePaint);

        Paint textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(11);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText(summaryText, right - 14, top + (height / 2f) + 4, textPaint);
    }

    private static String formatWeight(String weight, String measurement) {
        if (weight == null || weight.trim().isEmpty()) {
            return "-";
        }
        String w = weight.trim();
        if (measurement != null && !measurement.trim().isEmpty()) {
            return w + " " + measurement.trim();
        }
        return w;
    }

    private static double parseDoubleSafely(String val) {
        if (val == null) return 0.0;
        try {
            String clean = val.replaceAll("[^0-9.-]", "");
            if (clean.isEmpty() || clean.equals("-") || clean.equals(".")) return 0.0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
    }
}