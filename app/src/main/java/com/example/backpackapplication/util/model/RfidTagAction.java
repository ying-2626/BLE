package com.example.backpackapplication.util.model;

public class RfidTagAction {
    private int actionId;
    private String action;
    private String actionTime;
    private String actionUserId;
    private int command_id;
    private String delete_time;
    private String item_new_name;
    private String item_old_name;
    private String rfid_tag_id;

    // Getters
    public int getActionId() { return actionId; }
    public String getAction() { return action; }
    public String getActionTime() { return actionTime; }
    public String getActionUserId() { return actionUserId; }
    public String getRfidTagId() { return rfid_tag_id; }
    public int getCommandId() { return command_id; }
    public String getDeleteTime() { return delete_time; }
    public String getItemNewName() { return item_new_name; }
    public String getItemOldName() { return item_old_name; }
}
