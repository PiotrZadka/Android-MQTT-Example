package com.epmmu.mqttdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    //MQTT brokers
    public static final String BROKER_URL = "tcp://iot.eclipse.org:1883";
    //public static final String BROKER_URL = "tcp://broker.mqttdashboard.com:1883";

    //Unique Channel for Notifications
    private final String CHANNEL_ID = "personal_notification";
    Gson gson = new Gson();

    // Alter this to your student id
    String userid = "14056838";

    //We have to generate a unique Client id.
    String clientId = userid + "-sub2";

    // Name of topic that is being broadcasted through mqtt
    String sensorname = "doorState";
    String topicname = userid + "/" + sensorname;

    public final String SWITCH_STATE = userid +"/doorState";

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Switch mainSwitch = findViewById(R.id.mainSwitch);
        mainSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Card reader class
                final cardReaderData cardReader = new cardReaderData("unknown","unknown","unknown", "unknown");

                //Controls for switch button in app when using manually.
                if(mainSwitch.isChecked()){
                    // Hardcoded card name
                    cardReader.setTagId("card");
                    //Validate card
                    cardReaderData validationResult = (validateCard(cardReader));
                    if(validationResult.getMotorId()!= null){
                        validationResult.setDoorState("open");
                        String cardReaderJson = gson.toJson(validationResult);
                        publishSwitchState(cardReaderJson);
                        Toast.makeText(MainActivity.this, "DoorID '"+validationResult.getMotorId()+"' Opened", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Access for tag id "+validationResult.getTagId()+" is DISABLED", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    // Hardcoded card name
                    cardReader.setTagId("card");
                    // Same as above but for switching off.
                    //Validate card
                    cardReaderData validationResult = (validateCard(cardReader));
                    if(validationResult.getMotorId()!= null){
                        validationResult.setDoorState("close");
                        String cardReaderJson = gson.toJson(validationResult);
                        publishSwitchState(cardReaderJson);
                        Toast.makeText(MainActivity.this, "DoorID '"+validationResult.getMotorId()+"' Closed", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Access for tag id '"+validationResult.getTagId()+"' is DISABLED", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Create MQTT client and start subscribing to message queue
        try {
            // Change from original.
            // Messages in "null" are not stored
            mqttClient = new MqttClient(BROKER_URL, clientId, null);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    //This is called when the connection is lost. We could reconnect here.
                }

                @Override
                public void messageArrived(String topic, final MqttMessage message) {

                    // Checking what message arrived for DEBUG purposes
                    System.out.println("DEBUG: Message arrived. Topic: " + topic + "  Message: " + message.toString());
                    final cardReaderData messageJson = gson.fromJson(message.toString(), cardReaderData.class);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            System.out.println("Updating UI");

                            // Update UI elements when receiving message from MQTT.
                            // This are not controls for manual switch button.
                            // For manual controls check above.
                            // Statement is hardcoded for specific card. Same as above this could use some kind of NFC sync to establish card name at the beginning to tie up both door lock and card

                            // If card is recognised, open the door and send notification
                            if(messageJson.getTagId().equals("card") && messageJson.getDoorState().equals("open")){
                                mainSwitch.setChecked(true);
                                openNotification(true, messageJson);
                            }
                            // If card is recognised, close the door and send notification
                            else if(messageJson.getTagId().equals("card") && messageJson.getDoorState().equals("close")){
                                mainSwitch.setChecked(false);
                                openNotification(false, messageJson);
                            }
                            // if card is not recognised (someone is trying to get access), close door and send notification about false attempt being made.
                            else{
                                mainSwitch.setChecked(false);
                                openNotification(false, messageJson);
                                // Perhaps add notification to DB about false attempt
                            }
                        }
                    });
                    if ((topicname + "/LWT").equals(topic)) {
                        System.err.println("Sensor gone!");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //no-op
                }

                @Override
                public void connectComplete(boolean b, String s) {
                    //no-op
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // temp use of ThreadPolicy until use AsyncTask
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        startSubscribing();
    }


    public void startSubscribing() {
        try {

            mqttClient.connect();
            //Subscribe to all subtopics of home
            final String topic = topicname;
            mqttClient.subscribe(topic);

            System.out.println("Subscriber is now listening to " + topic);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Publish message to doorlock (servo motor)
    private void publishSwitchState(String switchState) {
        try {
            final MqttTopic switchStateTopic = mqttClient.getTopic(SWITCH_STATE);

            switchStateTopic.publish(new MqttMessage(switchState.getBytes()));

            System.out.println("Published data. Topic: " + switchStateTopic.getName() + "  Message: " + switchState);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Notification builder
    public void openNotification(Boolean DoorState, cardReaderData cardName) {
        // Build notification
        NotificationManager mNotificationManager;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext().getApplicationContext(), "notify_001");
        Intent ii = new Intent(getBaseContext().getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, ii, 0);

        // Set notification settings like icon/title/content
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle("Access Granted");
        if(DoorState && cardName.getTagId().equals("card")){
            mBuilder.setContentText("Front door opened using card name => "+cardName.getTagId()+" <=");
        }else if(!DoorState && cardName.getTagId().equals("card")){
            mBuilder.setContentText("Front door closed using card name => "+cardName.getTagId()+" <=");
        }
        else{
            mBuilder.setContentTitle("Access Denied");
            mBuilder.setContentText("Someone tried to open door using card name => "+cardName.getTagId()+" <=");
        }
        mBuilder.setPriority(Notification.PRIORITY_MAX);

        mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "YOUR_CHANNEL_ID";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }
        mNotificationManager.notify(0, mBuilder.build());
    }

    public cardReaderData validateCard(cardReaderData data) {

        //I have been using my own device over wifi and my local host was 192.168.0.11
        //If this is going to be run on virtual device in Android studio change address to 10.0.2.2
        String sensorServerURL = "http://192.168.0.11:8080/AssignmentServer/CardValidator";
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String dataToJson = gson.toJson(data);
        try {
            dataToJson = URLEncoder.encode(dataToJson, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        String fullURL = sensorServerURL + "?validationData="+dataToJson;
        System.out.println("Sending data to: "+fullURL);  // DEBUG confirmation message
        String line;
        String result = "";
        try {
            url = new URL(fullURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            // Request response from server to enable URL to be opened
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        cardReaderData resultData = gson.fromJson(result, cardReaderData.class);
        return resultData;
    }
}
