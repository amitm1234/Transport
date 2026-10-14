package com.example.transport1;

public class CashRecord {
    public String id;
    public String sourceRecordId;
    public String personName;
    public String refNo;
    public String date;
    public String vehicle;
    public String factory;
    public String measurement;
    public String weight;
    public String amount;

    public CashRecord() {}

    public CashRecord(String id, String sourceRecordId, String personName, String refNo, String date, String vehicle, String factory, String measurement, String weight, String amount) {
        this.id = id;
        this.sourceRecordId = sourceRecordId;
        this.personName = personName;
        this.refNo = refNo;
        this.date = date;
        this.vehicle = vehicle;
        this.factory = factory;
        this.measurement = measurement;
        this.weight = weight;
        this.amount = amount;
    }
}
