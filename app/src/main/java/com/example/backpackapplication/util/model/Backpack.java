package com.example.backpackapplication.util.model;

// BackpackItem.java
public class Backpack {
        private String backpackId;
        private String backpackName;
        private double backpackBattery;
        private String activationTime;
        private String deleteTime;
        private String ipAddress;
        private String location;
        private String macAddress;
        private int networkStatus;
        private int rfidTagNum;
        private String userId;


        // Getters
        public String getBackpackId() { return backpackId; }
        public String getBackpackName() { return backpackName; }
        public double getBackpackBattery() { return backpackBattery; }
    public String getFormattedBattery() {
        return String.format("%.1f%%", backpackBattery);
    }

    public int getRfidTagNum() { return rfidTagNum; }
    public String getActivationTime(){return activationTime;}
    public String getDeleteTime(){return deleteTime;}
    public String getLocation(){return location;}
    public int getNetworkStatus(){return networkStatus;}


}

