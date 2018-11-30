package com.epmmu.mqttdemo;

public class cardReaderData {
    String tagId;
    String readerId;
    String motorId;
    String doorState;

    cardReaderData(String tagId, String readerId, String motorId, String doorState){
        this.tagId = tagId;
        this.readerId = readerId;
        this.motorId = motorId;
        this.doorState = doorState;

    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getDoorState() {
        return doorState;
    }

    public void setDoorState(String doorState) {
        this.doorState = doorState;
    }

    public String getMotorId() {
        return motorId;
    }

    public void setMotorId(String motorId) {
        this.motorId = motorId;
    }
}