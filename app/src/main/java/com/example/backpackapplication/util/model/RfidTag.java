package com.example.backpackapplication.util.model;

public class RfidTag {
    private String rfidTagId;
    private String itemName;
    private String backpackId;
    private String activationTime;
    private String deleteTime;
    private int rfidTagStatus;




    // Getters
    public String getRfidTagId() { return rfidTagId; }
    public String getItemName() { return itemName; }
    public String getBackpackId() { return backpackId; }
    public int getRfidTagStatus() { return rfidTagStatus; }
    public String getActivationTime() { return activationTime; }
    public String getDeleteTime() { return deleteTime; }
}
