package com.example.transport1;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

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

    // =========================================================================
    // FACTORY REPORT PDF GENERATION
    // =========================================================================

    public static String getCurrencySymbol(Paint paint) {
        if (paint != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (paint.hasGlyph("₹")) {
                    return "₹";
                }
            } catch (Exception ignored) {}
        }
        return "Rs. ";
    }

    public static String formatCurrency(double amount, Paint paint) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return getCurrencySymbol(paint) + formatter.format(amount);
    }

    public static String buildReportFileName(String entityName) {
        String safeName = (entityName != null && !entityName.trim().isEmpty())
                ? entityName.trim().replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "")
                : "Report";
        if (safeName.isEmpty()) {
            safeName = "Report";
        }
        return safeName + "_Report.pdf";
    }

    private static boolean savePdfToTransportFolder(Context context, PdfDocument pdfDocument, String fileName, StringBuilder outErrorMsg) {
        if (context == null || pdfDocument == null || fileName == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String targetRelativePath1 = Environment.DIRECTORY_DOWNLOADS + "/Transport/";
            String targetRelativePath2 = Environment.DIRECTORY_DOWNLOADS + "/Transport";

            Uri existingUri = null;
            String[] projection = new String[] { MediaStore.Downloads._ID };
            String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND (" +
                    MediaStore.Downloads.RELATIVE_PATH + "=? OR " +
                    MediaStore.Downloads.RELATIVE_PATH + "=?)";
            String[] selectionArgs = new String[] { fileName, targetRelativePath1, targetRelativePath2 };

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                    existingUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                }
            } catch (Exception ignored) {}

            if (existingUri != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(existingUri, "wt")) {
                    if (os != null) {
                        pdfDocument.writeTo(os);
                        return true;
                    }
                } catch (Exception e) {
                    try {
                        context.getContentResolver().delete(existingUri, null, null);
                    } catch (Exception ignored) {}
                    existingUri = null;
                }
            }

            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, targetRelativePath1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    values.put(MediaStore.Downloads.IS_PENDING, 1);
                }

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            pdfDocument.writeTo(os);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ContentValues pendingValues = new ContentValues();
                        pendingValues.put(MediaStore.Downloads.IS_PENDING, 0);
                        context.getContentResolver().update(uri, pendingValues, null, null);
                    }
                    return true;
                } else {
                    if (outErrorMsg != null) outErrorMsg.append("Unable to create MediaStore entry.");
                    return false;
                }
            } catch (Exception e) {
                if (outErrorMsg != null) outErrorMsg.append(e.getMessage() != null ? e.getMessage() : "Insert failed");
                return false;
            }

        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File transportDir = new File(downloadsDir, "Transport");
            if (!transportDir.exists()) {
                transportDir.mkdirs();
            }
            File pdfFile = new File(transportDir, fileName);
            try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
                pdfDocument.writeTo(outputStream);
                return true;
            } catch (Exception e) {
                if (outErrorMsg != null) outErrorMsg.append(e.getMessage() != null ? e.getMessage() : "File save failed");
                return false;
            }
        }
    }

    public static void generateFactoryPdf(Context context, String factoryName, List<TransportData> dataList, double totalAdvance, double totalUsed, double remainingAdvance) {
        if (context == null) return;

        if (dataList == null || dataList.isEmpty()) {
            Toast.makeText(context, "No transaction data", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Generating PDF in background...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            String fileName = buildReportFileName(factoryName);

            boolean success = false;
            StringBuilder errorBuffer = new StringBuilder();

            PdfDocument pdfDocument = new PdfDocument();

            try {
                int pageWidth = 595;
                int pageHeight = 842;
                int margin = 40;
                float left = margin;
                float right = pageWidth - margin; // 555
                float bottomLimit = 802; // 842 - 40
                int pageNumber = 1;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float y = margin;

                // 1. Title (center, bold, ~20sp)
                Paint titlePaint = new Paint();
                titlePaint.setColor(Color.parseColor("#1A1A1A"));
                titlePaint.setTextSize(20);
                titlePaint.setFakeBoldText(true);
                titlePaint.setTextAlign(Paint.Align.CENTER);

                String displayFactoryName = (factoryName != null && !factoryName.trim().isEmpty()) ? factoryName.trim() : "Factory";
                String titleText = "Factory Report: " + displayFactoryName;
                canvas.drawText(truncateText(titleText, titlePaint, right - left), pageWidth / 2f, y + 20, titlePaint);
                y += 28;

                // 2. Date Line (center, grey, ~11sp)
                Paint datePaint = new Paint();
                datePaint.setColor(Color.parseColor("#64748B"));
                datePaint.setTextSize(11);
                datePaint.setTextAlign(Paint.Align.CENTER);

                String currentDateStr = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(new Date());
                canvas.drawText("Generated on: " + currentDateStr, pageWidth / 2f, y + 12, datePaint);
                y += 28;

                // 3. Summary Box (Light grey-blue background, rounded corners, 3 equal columns)
                float summaryBoxHeight = 50f;
                Paint summaryBgPaint = new Paint();
                summaryBgPaint.setStyle(Paint.Style.FILL);
                summaryBgPaint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawRoundRect(left, y, right, y + summaryBoxHeight, 12, 12, summaryBgPaint);

                Paint sumLabelPaint = new Paint();
                sumLabelPaint.setColor(Color.parseColor("#64748B"));
                sumLabelPaint.setTextSize(10);
                sumLabelPaint.setTextAlign(Paint.Align.CENTER);

                Paint sumValPaint = new Paint();
                sumValPaint.setColor(Color.parseColor("#1A1A1A"));
                sumValPaint.setTextSize(12);
                sumValPaint.setFakeBoldText(true);
                sumValPaint.setTextAlign(Paint.Align.CENTER);

                float colWidth = (right - left) / 3f;
                float c1 = left + colWidth / 2f;
                float c2 = left + colWidth + colWidth / 2f;
                float c3 = left + colWidth * 2 + colWidth / 2f;

                canvas.drawText("Total Advance", c1, y + 18, sumLabelPaint);
                canvas.drawText("Total Used", c2, y + 18, sumLabelPaint);
                canvas.drawText("Remaining", c3, y + 18, sumLabelPaint);

                canvas.drawText(formatCurrency(totalAdvance, sumValPaint), c1, y + 36, sumValPaint);
                canvas.drawText(formatCurrency(totalUsed, sumValPaint), c2, y + 36, sumValPaint);

                Paint remValPaint = new Paint(sumValPaint);
                int remColor = (remainingAdvance >= 0) ? Color.parseColor("#166534") : Color.parseColor("#B91C1C");
                remValPaint.setColor(remColor);
                canvas.drawText(formatCurrency(remainingAdvance, remValPaint), c3, y + 36, remValPaint);

                // Dividers
                Paint summaryDividerPaint = new Paint();
                summaryDividerPaint.setColor(Color.parseColor("#E2E8F0"));
                summaryDividerPaint.setStrokeWidth(1f);
                canvas.drawLine(left + colWidth, y + 8, left + colWidth, y + summaryBoxHeight - 8, summaryDividerPaint);
                canvas.drawLine(left + colWidth * 2, y + 8, left + colWidth * 2, y + summaryBoxHeight - 8, summaryDividerPaint);

                y += summaryBoxHeight + 16f;

                // CARD CONSTANTS & LAYOUT SETUP
                int cardHeight = 56;
                float cardSpacing = 8f;

                Paint cardBgPaint = new Paint();
                cardBgPaint.setStyle(Paint.Style.FILL);
                cardBgPaint.setColor(Color.parseColor("#FFFFFF"));

                Paint headerStripPaint = new Paint();
                headerStripPaint.setStyle(Paint.Style.FILL);
                headerStripPaint.setColor(Color.parseColor("#F1F5F9"));

                Paint headerBorderPaint = new Paint();
                headerBorderPaint.setColor(Color.parseColor("#E2E8F0"));
                headerBorderPaint.setStrokeWidth(1f);

                Paint cardStrokePaint = new Paint();
                cardStrokePaint.setStyle(Paint.Style.STROKE);
                cardStrokePaint.setColor(Color.parseColor("#CBD5E1"));
                cardStrokePaint.setStrokeWidth(1.2f);

                Paint colHeaderPaint = new Paint();
                colHeaderPaint.setColor(Color.parseColor("#475569"));
                colHeaderPaint.setTextSize(9f);
                colHeaderPaint.setFakeBoldText(true);

                Paint cellTextPaint = new Paint();
                cellTextPaint.setColor(Color.parseColor("#1E293B"));
                cellTextPaint.setTextSize(9f);

                // 7 Columns X-Positions and Max-Widths
                // Printable width: left = 40, right = 555 (total width 515)
                float[] colX = new float[] { 48f, 124f, 192f, 278f, 336f, 402f, 547f };
                float[] colMaxWidth = new float[] { 72f, 64f, 82f, 54f, 62f, 58f, 75f };
                String[] colTitles = new String[] { "Vehicle", "Date", "Sell Person", "Weight", "Price", "GST", "Total" };

                // 4. Transaction Cards Loop
                for (TransportData data : dataList) {
                    if (data == null) continue;

                    // Pagination check
                    if (y + cardHeight > bottomLimit) {
                        pdfDocument.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = margin;
                    }

                    float cardTop = y;
                    float cardBottom = cardTop + cardHeight;

                    // 1. Base Card Background
                    canvas.drawRoundRect(left, cardTop, right, cardBottom, 8, 8, cardBgPaint);

                    // 2. Tinted Header Strip (Row 1)
                    float headerHeight = 24f;
                    canvas.drawRoundRect(left, cardTop, right, cardTop + headerHeight + 4, 8, 8, headerStripPaint);
                    canvas.drawRect(left, cardTop + headerHeight - 4, right, cardTop + headerHeight, headerStripPaint);
                    canvas.drawLine(left, cardTop + headerHeight, right, cardTop + headerHeight, headerBorderPaint);

                    // Column Headings
                    float headerY = cardTop + 16f;
                    for (int i = 0; i < colTitles.length; i++) {
                        if (i == 6) {
                            colHeaderPaint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
                        } else {
                            colHeaderPaint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
                        }
                    }

                    // 3. Row 2 Values
                    float valueY = cardTop + 42f;

                    String weightVal = (data.buyWeight != null && !data.buyWeight.trim().isEmpty()) ? data.buyWeight : data.weight;
                    String formattedWeight = formatWeight(weightVal, data.measurement);

                    String valVehicle = truncateText(data.vehicle, cellTextPaint, colMaxWidth[0]);
                    String valDate = truncateText(data.date, cellTextPaint, colMaxWidth[1]);
                    String valPerson = truncateText(data.sellPerson, cellTextPaint, colMaxWidth[2]);
                    String valWeight = truncateText(formattedWeight, cellTextPaint, colMaxWidth[3]);
                    String valPrice = truncateText(formatCurrencyVal(data.buyPrice, cellTextPaint), cellTextPaint, colMaxWidth[4]);
                    String valGST = truncateText(formatCurrencyVal(data.buyGST, cellTextPaint), cellTextPaint, colMaxWidth[5]);
                    String valTotal = truncateText(formatCurrencyVal(data.buyTotalAmount, cellTextPaint), cellTextPaint, colMaxWidth[6]);

                    String[] vals = new String[] { valVehicle, valDate, valPerson, valWeight, valPrice, valGST, valTotal };

                    for (int i = 0; i < vals.length; i++) {
                        if (i == 6) {
                            cellTextPaint.setFakeBoldText(true);
                            cellTextPaint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                        } else {
                            cellTextPaint.setFakeBoldText(false);
                            cellTextPaint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                        }
                    }

                    // 4. Card Outer Border
                    canvas.drawRoundRect(left, cardTop, right, cardBottom, 8, 8, cardStrokePaint);

                    y = cardBottom + cardSpacing;
                }

                pdfDocument.finishPage(page);

                // Save File
                success = savePdfToTransportFolder(context, pdfDocument, fileName, errorBuffer);

            } catch (Exception e) {
                e.printStackTrace();
                if (errorBuffer.length() == 0) {
                    errorBuffer.append(e.getMessage() != null ? e.getMessage() : "Error saving PDF");
                }
            } finally {
                try {
                    pdfDocument.close();
                } catch (Exception ignored) {}
            }

            final boolean finalSuccess = success;
            final String finalErrorMsg = errorBuffer.toString();

            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    Toast.makeText(context, "PDF saved in Downloads/Transport", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error saving PDF: " + (finalErrorMsg.isEmpty() ? "Unknown error" : finalErrorMsg), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // =========================================================================
    // SELL PERSON REPORT PDF GENERATION
    // =========================================================================

    public static void generateSellPersonPdf(Context context, String personName, List<TransportData> dataList, double totalPrice, double totalGST, double totalAmount) {
        if (context == null) return;

        if (dataList == null || dataList.isEmpty()) {
            Toast.makeText(context, "No transaction data", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Generating PDF in background...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            String fileName = buildReportFileName(personName);

            boolean success = false;
            StringBuilder errorBuffer = new StringBuilder();

            PdfDocument pdfDocument = new PdfDocument();

            try {
                int pageWidth = 595;
                int pageHeight = 842;
                int margin = 40;
                float left = margin;
                float right = pageWidth - margin; // 555
                float bottomLimit = 802; // 842 - 40
                int pageNumber = 1;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float y = margin;

                // 1. Title (center, bold, ~20sp)
                Paint titlePaint = new Paint();
                titlePaint.setColor(Color.parseColor("#1A1A1A"));
                titlePaint.setTextSize(20);
                titlePaint.setFakeBoldText(true);
                titlePaint.setTextAlign(Paint.Align.CENTER);

                String displayPersonName = (personName != null && !personName.trim().isEmpty()) ? personName.trim() : "Sell Person";
                String titleText = "Sell Person Report: " + displayPersonName;
                canvas.drawText(truncateText(titleText, titlePaint, right - left), pageWidth / 2f, y + 20, titlePaint);
                y += 28;

                // 2. Date Line (center, grey, ~11sp)
                Paint datePaint = new Paint();
                datePaint.setColor(Color.parseColor("#64748B"));
                datePaint.setTextSize(11);
                datePaint.setTextAlign(Paint.Align.CENTER);

                String currentDateStr = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(new Date());
                canvas.drawText("Generated on: " + currentDateStr, pageWidth / 2f, y + 12, datePaint);
                y += 28;

                // 3. Summary Box (Light grey-blue background, rounded corners, 3 equal columns: Total Price | Total GST | Total Amount)
                float summaryBoxHeight = 50f;
                Paint summaryBgPaint = new Paint();
                summaryBgPaint.setStyle(Paint.Style.FILL);
                summaryBgPaint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawRoundRect(left, y, right, y + summaryBoxHeight, 12, 12, summaryBgPaint);

                Paint sumLabelPaint = new Paint();
                sumLabelPaint.setColor(Color.parseColor("#64748B"));
                sumLabelPaint.setTextSize(10);
                sumLabelPaint.setTextAlign(Paint.Align.CENTER);

                Paint sumValPaint = new Paint();
                sumValPaint.setColor(Color.parseColor("#1A1A1A"));
                sumValPaint.setTextSize(12);
                sumValPaint.setFakeBoldText(true);
                sumValPaint.setTextAlign(Paint.Align.CENTER);

                float colWidth = (right - left) / 3f;
                float c1 = left + colWidth / 2f;
                float c2 = left + colWidth + colWidth / 2f;
                float c3 = left + colWidth * 2 + colWidth / 2f;

                canvas.drawText("Total Price", c1, y + 18, sumLabelPaint);
                canvas.drawText("Total GST", c2, y + 18, sumLabelPaint);
                canvas.drawText("Total Amount", c3, y + 18, sumLabelPaint);

                canvas.drawText(formatCurrency(totalPrice, sumValPaint), c1, y + 36, sumValPaint);
                canvas.drawText(formatCurrency(totalGST, sumValPaint), c2, y + 36, sumValPaint);
                canvas.drawText(formatCurrency(totalAmount, sumValPaint), c3, y + 36, sumValPaint);

                // Dividers
                Paint summaryDividerPaint = new Paint();
                summaryDividerPaint.setColor(Color.parseColor("#E2E8F0"));
                summaryDividerPaint.setStrokeWidth(1f);
                canvas.drawLine(left + colWidth, y + 8, left + colWidth, y + summaryBoxHeight - 8, summaryDividerPaint);
                canvas.drawLine(left + colWidth * 2, y + 8, left + colWidth * 2, y + summaryBoxHeight - 8, summaryDividerPaint);

                y += summaryBoxHeight + 16f;

                // CARD CONSTANTS & LAYOUT SETUP
                int cardHeight = 56;
                float cardSpacing = 8f;

                Paint cardBgPaint = new Paint();
                cardBgPaint.setStyle(Paint.Style.FILL);
                cardBgPaint.setColor(Color.parseColor("#FFFFFF"));

                Paint headerStripPaint = new Paint();
                headerStripPaint.setStyle(Paint.Style.FILL);
                headerStripPaint.setColor(Color.parseColor("#F1F5F9"));

                Paint headerBorderPaint = new Paint();
                headerBorderPaint.setColor(Color.parseColor("#E2E8F0"));
                headerBorderPaint.setStrokeWidth(1f);

                Paint cardStrokePaint = new Paint();
                cardStrokePaint.setStyle(Paint.Style.STROKE);
                cardStrokePaint.setColor(Color.parseColor("#CBD5E1"));
                cardStrokePaint.setStrokeWidth(1.2f);

                Paint colHeaderPaint = new Paint();
                colHeaderPaint.setColor(Color.parseColor("#475569"));
                colHeaderPaint.setTextSize(9f);
                colHeaderPaint.setFakeBoldText(true);

                Paint cellTextPaint = new Paint();
                cellTextPaint.setColor(Color.parseColor("#1E293B"));
                cellTextPaint.setTextSize(9f);

                // Sort transactions chronologically by date
                Collections.sort(dataList, (d1, d2) -> PersonReportActivity.compareDates(d1.date, d2.date));

                // 8 Columns X-Positions and Max-Widths for Sell Person Report (including No.)
                float[] colX = new float[] { 48f, 78f, 142f, 204f, 282f, 342f, 406f, 547f };
                float[] colMaxWidth = new float[] { 26f, 60f, 58f, 74f, 56f, 60f, 58f, 72f };
                String[] colTitles = new String[] { "No.", "Vehicle", "Date", "Factory", "Weight", "Price", "GST", "Total" };

                int itemIndex = 1;

                // 4. Transaction Cards Loop
                for (TransportData data : dataList) {
                    if (data == null) continue;

                    // Pagination check
                    if (y + cardHeight > bottomLimit) {
                        pdfDocument.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = margin;
                    }

                    float cardTop = y;
                    float cardBottom = cardTop + cardHeight;

                    // 1. Base Card Background
                    canvas.drawRoundRect(left, cardTop, right, cardBottom, 8, 8, cardBgPaint);

                    // 2. Tinted Header Strip (Row 1)
                    float headerHeight = 24f;
                    canvas.drawRoundRect(left, cardTop, right, cardTop + headerHeight + 4, 8, 8, headerStripPaint);
                    canvas.drawRect(left, cardTop + headerHeight - 4, right, cardTop + headerHeight, headerStripPaint);
                    canvas.drawLine(left, cardTop + headerHeight, right, cardTop + headerHeight, headerBorderPaint);

                    // Column Headings
                    float headerY = cardTop + 16f;
                    for (int i = 0; i < colTitles.length; i++) {
                        if (i == 7) {
                            colHeaderPaint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
                        } else {
                            colHeaderPaint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
                        }
                    }

                    // 3. Row 2 Values
                    float valueY = cardTop + 42f;

                    String weightVal = (data.sellWeight != null && !data.sellWeight.trim().isEmpty()) ? data.sellWeight : data.weight;
                    String formattedWeight = formatWeight(weightVal, data.measurement);

                    String valNo = String.valueOf(itemIndex++);
                    String valVehicle = truncateText(data.vehicle, cellTextPaint, colMaxWidth[1]);
                    String valDate = truncateText(data.date, cellTextPaint, colMaxWidth[2]);
                    String valFactory = truncateText(data.factory, cellTextPaint, colMaxWidth[3]);
                    String valWeight = truncateText(formattedWeight, cellTextPaint, colMaxWidth[4]);
                    String valPrice = truncateText(formatCurrencyVal(data.sellPrice, cellTextPaint), cellTextPaint, colMaxWidth[5]);
                    String valGST = truncateText(formatCurrencyVal(data.sellGST, cellTextPaint), cellTextPaint, colMaxWidth[6]);
                    String valTotal = truncateText(formatCurrencyVal(data.sellTotalAmount, cellTextPaint), cellTextPaint, colMaxWidth[7]);

                    String[] vals = new String[] { valNo, valVehicle, valDate, valFactory, valWeight, valPrice, valGST, valTotal };

                    for (int i = 0; i < vals.length; i++) {
                        if (i == 7) {
                            cellTextPaint.setFakeBoldText(true);
                            cellTextPaint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                        } else {
                            cellTextPaint.setFakeBoldText(false);
                            cellTextPaint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                        }
                    }

                    // 4. Card Outer Border
                    canvas.drawRoundRect(left, cardTop, right, cardBottom, 8, 8, cardStrokePaint);

                    y = cardBottom + cardSpacing;
                }

                pdfDocument.finishPage(page);

                // Save File
                success = savePdfToTransportFolder(context, pdfDocument, fileName, errorBuffer);

            } catch (Exception e) {
                e.printStackTrace();
                if (errorBuffer.length() == 0) {
                    errorBuffer.append(e.getMessage() != null ? e.getMessage() : "Error saving PDF");
                }
            } finally {
                try {
                    pdfDocument.close();
                } catch (Exception ignored) {}
            }

            final boolean finalSuccess = success;
            final String finalErrorMsg = errorBuffer.toString();

            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    Toast.makeText(context, "PDF saved in Downloads/Transport", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error saving PDF: " + (finalErrorMsg.isEmpty() ? "Unknown error" : finalErrorMsg), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // =========================================================================
    // CASH RECORDS PDF GENERATION
    // =========================================================================

    public static void generateCashPdf(Context context, List<CashRecord> cashRecordList, String selectedPerson) {
        if (context == null) return;

        if (cashRecordList == null || cashRecordList.isEmpty()) {
            Toast.makeText(context, "No transaction data", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Generating PDF in background...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            String fileName = "Cash_Report.pdf";

            boolean success = false;
            StringBuilder errorBuffer = new StringBuilder();

            PdfDocument pdfDocument = new PdfDocument();

            try {
                int pageWidth = 595;
                int pageHeight = 842;
                int margin = 40;
                float left = margin;
                float right = pageWidth - margin; // 555
                float bottomLimit = 802;
                int pageNumber = 1;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float y = margin;

                // 1. Title
                Paint titlePaint = new Paint();
                titlePaint.setColor(Color.parseColor("#1A1A1A"));
                titlePaint.setTextSize(20);
                titlePaint.setFakeBoldText(true);
                titlePaint.setTextAlign(Paint.Align.CENTER);

                String titleText;
                if (selectedPerson == null || selectedPerson.trim().isEmpty() || "All".equalsIgnoreCase(selectedPerson.trim())) {
                    titleText = "Cash Records Report";
                } else {
                    titleText = "Cash Records: " + selectedPerson.trim();
                }

                canvas.drawText(truncateText(titleText, titlePaint, right - left), pageWidth / 2f, y + 20, titlePaint);
                y += 28;

                // 2. Date Line
                Paint datePaint = new Paint();
                datePaint.setColor(Color.parseColor("#64748B"));
                datePaint.setTextSize(11);
                datePaint.setTextAlign(Paint.Align.CENTER);

                String currentDateStr = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(new Date());
                canvas.drawText("Generated on: " + currentDateStr, pageWidth / 2f, y + 12, datePaint);
                y += 28;

                // Group cash records by personName
                Map<String, List<CashRecord>> groupedMap = new LinkedHashMap<>();
                for (CashRecord r : cashRecordList) {
                    if (r == null) continue;
                    String pName = (r.personName != null && !r.personName.trim().isEmpty()) ? r.personName.trim() : "Other";
                    List<CashRecord> list = groupedMap.get(pName);
                    if (list == null) {
                        list = new ArrayList<>();
                        groupedMap.put(pName, list);
                    }
                    list.add(r);
                }

                // Table Layout Paints
                Paint groupHeaderPaint = new Paint();
                groupHeaderPaint.setColor(Color.parseColor("#0F172A"));
                groupHeaderPaint.setTextSize(12f);
                groupHeaderPaint.setFakeBoldText(true);

                Paint tableHeaderBgPaint = new Paint();
                tableHeaderBgPaint.setStyle(Paint.Style.FILL);
                tableHeaderBgPaint.setColor(Color.parseColor("#F1F5F9"));

                Paint colHeaderPaint = new Paint();
                colHeaderPaint.setColor(Color.parseColor("#475569"));
                colHeaderPaint.setTextSize(9f);
                colHeaderPaint.setFakeBoldText(true);

                Paint cellTextPaint = new Paint();
                cellTextPaint.setColor(Color.parseColor("#1E293B"));
                cellTextPaint.setTextSize(9f);

                Paint linePaint = new Paint();
                linePaint.setColor(Color.parseColor("#E2E8F0"));
                linePaint.setStrokeWidth(1f);

                float[] colX = new float[] { 48f, 85f, 140f, 210f, 300f, 390f, 547f };
                float[] colMaxWidth = new float[] { 32f, 50f, 65f, 85f, 85f, 65f, 80f };
                String[] colTitles = new String[] { "No.", "Ref No.", "Date", "Vehicle", "Factory", "Weight", "Total" };

                double grandTotal = 0;

                for (Map.Entry<String, List<CashRecord>> entry : groupedMap.entrySet()) {
                    String personGroup = entry.getKey();
                    List<CashRecord> records = entry.getValue();

                    // Sort group records by date
                    Collections.sort(records, (r1, r2) -> PersonReportActivity.compareDates(r1.date, r2.date));

                    // Check page limit before starting group header
                    if (y + 50 > bottomLimit) {
                        pdfDocument.finishPage(page);
                        pageNumber++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = margin;
                    }

                    // Draw Group Header
                    canvas.drawText("Person: " + personGroup, left + 4, y + 14, groupHeaderPaint);
                    y += 20f;

                    // Draw Table Header Row
                    y = drawCashTableHeader(canvas, left, y, right, colTitles, colX, colHeaderPaint, tableHeaderBgPaint, linePaint);

                    double personTotal = 0;
                    int seqNo = 1;

                    for (CashRecord record : records) {
                        if (record == null) continue;

                        double amt = parseDoubleSafely(record.amount);
                        personTotal += amt;

                        // Row height ~ 20f
                        if (y + 20f > bottomLimit) {
                            pdfDocument.finishPage(page);
                            pageNumber++;
                            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                            page = pdfDocument.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = margin;

                            // Draw continuation header
                            canvas.drawText("Person: " + personGroup + " (continued)", left + 4, y + 14, groupHeaderPaint);
                            y += 20f;
                            y = drawCashTableHeader(canvas, left, y, right, colTitles, colX, colHeaderPaint, tableHeaderBgPaint, linePaint);
                        }

                        float valueY = y + 14f;

                        String formattedWeight = formatWeight(record.weight, record.measurement);
                        String valNo = String.valueOf(seqNo++);
                        String valRef = truncateText(record.refNo, cellTextPaint, colMaxWidth[1]);
                        String valDate = truncateText(record.date, cellTextPaint, colMaxWidth[2]);
                        String valVehicle = truncateText(record.vehicle, cellTextPaint, colMaxWidth[3]);
                        String valFactory = truncateText(record.factory, cellTextPaint, colMaxWidth[4]);
                        String valWeight = truncateText(formattedWeight, cellTextPaint, colMaxWidth[5]);
                        String valTotal = truncateText(formatCurrency(amt, cellTextPaint), cellTextPaint, colMaxWidth[6]);

                        String[] vals = new String[] { valNo, valRef, valDate, valVehicle, valFactory, valWeight, valTotal };

                        for (int i = 0; i < vals.length; i++) {
                            if (i == 6) {
                                cellTextPaint.setFakeBoldText(true);
                                cellTextPaint.setTextAlign(Paint.Align.RIGHT);
                                canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                            } else {
                                cellTextPaint.setFakeBoldText(false);
                                cellTextPaint.setTextAlign(Paint.Align.LEFT);
                                canvas.drawText(vals[i], colX[i], valueY, cellTextPaint);
                            }
                        }

                        y += 20f;
                        canvas.drawLine(left, y, right, y, linePaint);
                    }

                    grandTotal += personTotal;

                    // Person Total Line
                    y += 4f;
                    Paint pTotalPaint = new Paint();
                    pTotalPaint.setColor(Color.parseColor("#0F172A"));
                    pTotalPaint.setTextSize(10f);
                    pTotalPaint.setFakeBoldText(true);
                    pTotalPaint.setTextAlign(Paint.Align.RIGHT);

                    String pTotalStr = personGroup + " Total: " + formatCurrency(personTotal, pTotalPaint);
                    canvas.drawText(pTotalStr, right - 4, y + 12f, pTotalPaint);
                    y += 24f;
                }

                // Grand Total Box at the end
                if (y + 36f > bottomLimit) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }

                float gBoxHeight = 32f;
                Paint gBgPaint = new Paint();
                gBgPaint.setStyle(Paint.Style.FILL);
                gBgPaint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawRoundRect(left, y, right, y + gBoxHeight, 8, 8, gBgPaint);

                Paint gStrokePaint = new Paint();
                gStrokePaint.setStyle(Paint.Style.STROKE);
                gStrokePaint.setColor(Color.parseColor("#CBD5E1"));
                gStrokePaint.setStrokeWidth(1.2f);
                canvas.drawRoundRect(left, y, right, y + gBoxHeight, 8, 8, gStrokePaint);

                Paint gTextPaint = new Paint();
                gTextPaint.setColor(Color.parseColor("#16A34A"));
                gTextPaint.setTextSize(12f);
                gTextPaint.setFakeBoldText(true);
                gTextPaint.setTextAlign(Paint.Align.RIGHT);

                canvas.drawText("Grand Total: " + formatCurrency(grandTotal, gTextPaint), right - 12, y + 20f, gTextPaint);

                pdfDocument.finishPage(page);

                // Save File
                success = savePdfToTransportFolder(context, pdfDocument, fileName, errorBuffer);

            } catch (Exception e) {
                e.printStackTrace();
                if (errorBuffer.length() == 0) {
                    errorBuffer.append(e.getMessage() != null ? e.getMessage() : "Error saving PDF");
                }
            } finally {
                try {
                    pdfDocument.close();
                } catch (Exception ignored) {}
            }

            final boolean finalSuccess = success;
            final String finalErrorMsg = errorBuffer.toString();

            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    Toast.makeText(context, "PDF saved in Downloads/Transport", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error saving PDF: " + (finalErrorMsg.isEmpty() ? "Unknown error" : finalErrorMsg), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private static float drawCashTableHeader(Canvas canvas, float left, float top, float right, String[] colTitles, float[] colX, Paint colHeaderPaint, Paint bgPaint, Paint linePaint) {
        float hHeight = 22f;
        canvas.drawRect(left, top, right, top + hHeight, bgPaint);
        canvas.drawLine(left, top + hHeight, right, top + hHeight, linePaint);

        float headerY = top + 15f;
        for (int i = 0; i < colTitles.length; i++) {
            if (i == 6) {
                colHeaderPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
            } else {
                colHeaderPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(colTitles[i], colX[i], headerY, colHeaderPaint);
            }
        }
        return top + hHeight;
    }

    private static String formatCurrencyVal(String valStr, Paint paint) {
        if (valStr == null || valStr.trim().isEmpty()) {
            return "-";
        }
        double val = parseDoubleSafely(valStr);
        return formatCurrency(val, paint);
    }

    private static String truncateText(String text, Paint paint, float maxWidth) {
        if (text == null || text.trim().isEmpty()) return "-";
        String str = text.trim();
        if (paint.measureText(str) <= maxWidth) {
            return str;
        }
        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        int len = str.length();
        while (len > 0 && (paint.measureText(str.substring(0, len)) + ellipsisWidth) > maxWidth) {
            len--;
        }
        return (len > 0) ? str.substring(0, len) + ellipsis : ellipsis;
    }
}