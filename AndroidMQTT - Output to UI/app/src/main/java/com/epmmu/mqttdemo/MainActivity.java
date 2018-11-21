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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class MainActivity extends AppCompatActivity {

    public static final String BROKER_URL = "tcp://iot.eclipse.org:1883";
    //public static final String BROKER_URL = "tcp://broker.mqttdashboard.com:1883";

    private final String CHANNEL_ID = "personal_notification";


    String userid = "14056838";  // Alter this to your student id
    //We have to generate a unique Client id.
    String clientId = userid + "-sub2";

    // Default sensor to listen for -
    // Change to another if you are broadcasting a different sensor name
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
                if(mainSwitch.isChecked()){
                    publishSwitchState("open");
                    Toast.makeText(MainActivity.this, "Main Door Open", Toast.LENGTH_SHORT).show();
                }
                else{
                    publishSwitchState("close");
                    Toast.makeText(MainActivity.this, "Main Door Closed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // start new
        // Create MQTT client and start subscribing to message queue
        try {
            // change from original. Messages in "null" are not stored
            mqttClient = new MqttClient(BROKER_URL, clientId, null);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    //This is called when the connection is lost. We could reconnect here.
                }

                @Override
                public void messageArrived(String topic, final MqttMessage message) {
                    System.out.println("DEBUG: Message arrived. Topic: " + topic + "  Message: " + message.toString());
                    // get message data
                    System.out.println(message);
                    final String messageStr = message.toString();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            System.out.println("Updating UI");
                            // Update UI elements
                            if(messageStr.equals("open")){
                                mainSwitch.setChecked(true);
                                openNotification(true);
                            }
                            else{
                                mainSwitch.setChecked(false);
                                openNotification(false);
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

    public void openNotification(Boolean DoorState) {
        // Build notification
        NotificationManager mNotificationManager;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext().getApplicationContext(), "notify_001");
        Intent ii = new Intent(getBaseContext().getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, ii, 0);


        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle("Access Granted");
        if(DoorState){
            mBuilder.setContentText("Front door opened using card <card id>");
        }else{
            mBuilder.setContentText("Front door closed using card <card id>");
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

}
