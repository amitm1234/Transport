package com.example.transport1;

public class TransportData {

    // Common fields
    public String vehicle;
    public String date;
    public String factory;
    public String measurement;
    public String weight;

    // Buy fields
    public String buyWeight;
    public String buyPrice;
    public String buyGST;
    public String buyTotalAmount;
    public String buyGSTPercent;

    // Sell fields
    public String sellPerson;
    public String sellWeight;
    public String sellPrice;
    public String sellGST;
    public String sellTotalAmount;
    public String sellGSTPercent;

    // Empty constructor for Firebase
    public TransportData() {}

    // Constructor with all fields
    public TransportData(String vehicle, String date, String factory, String measurement, String weight,
                         String buyWeight, String buyPrice, String buyGST, String buyTotalAmount, String buyGSTPercent,
                         String sellPerson, String sellWeight, String sellPrice, String sellGST, String sellTotalAmount, String sellGSTPercent) {
        this.vehicle = vehicle;
        this.date = date;
        this.factory = factory;
        this.measurement = measurement;
        this.weight = weight;

        this.buyWeight = buyWeight;
        this.buyPrice = buyPrice;
        this.buyGST = buyGST;
        this.buyTotalAmount = buyTotalAmount;
        this.buyGSTPercent = buyGSTPercent;

        this.sellPerson = sellPerson;
        this.sellWeight = sellWeight;
        this.sellPrice = sellPrice;
        this.sellGST = sellGST;
        this.sellTotalAmount = sellTotalAmount;
        this.sellGSTPercent = sellGSTPercent;
    }
}