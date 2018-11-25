package com.epmmu.mqttdemo;

public class cardReaderData {
    String tagId;
    String decideID;
    String doorState;

    cardReaderData(String tagId, String decideID, String doorState){
        this.tagId = tagId;
        this.decideID = decideID;
        this.doorState = doorState;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getReaderId() {
        return decideID;
    }

    public void setReaderId(String deviceID) {
        this.decideID = deviceID;
    }

    public String getDoorState() {
        return doorState;
    }

    public void setDoorState(String doorState) {
        this.doorState = doorState;
    }
}
