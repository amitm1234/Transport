package com.example.transport1;

import java.util.ArrayList;
import java.util.List;

public class PdfDataHolder {

    private static final List<TransportData> dataList = new ArrayList<>();

    public static void addData(TransportData data) {
        dataList.add(data);
    }

    public static List<TransportData> getDataList() {
        return new ArrayList<>(dataList);
    }

    public static void clearDataList() {
        dataList.clear();
    }
}