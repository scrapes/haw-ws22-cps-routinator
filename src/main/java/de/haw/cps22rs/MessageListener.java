package de.haw.cps22rs;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.graphhopper.util.shapes.GHPoint3D;
import com.graphhopper.util.PointList;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;

import org.json.*;

import java.util.Arrays;
import java.util.Objects;

public class MessageListener implements IMqttMessageListener{
    private final GraphHopper _hopper;
    private final MqttClient _client;

    private PointList routing(double fromLat, double fromLon, double toLat, double toLon) {
        // simple configuration of the request object
        GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile("car");
        GHResponse rsp = _hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
            throw new RuntimeException(rsp.getErrors().toString());

        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();
        return path.getPoints();
    }


    public MessageListener(GraphHopper hopper, MqttClient client){
        _hopper = hopper;
        _client = client;
    }

    private void routeRequest(MqttMessage mqttMessage) throws MqttException {
        JSONObject message = new JSONObject(Arrays.toString(mqttMessage.getPayload()));
        System.out.println(Arrays.toString(mqttMessage.getPayload()));
        double fromLat, fromLon, toLat, toLon;
        String uuid;
        try {
            uuid = message.getString("uuid");
            fromLat = message.getJSONObject("from").getDouble("lat");
            fromLon = message.getJSONObject("from").getDouble("lon");
            toLat = message.getJSONObject("to").getDouble("lat");
            toLon = message.getJSONObject("to").getDouble("lon");
        }
        catch (Exception ee){
            System.err.println("Messages malformed!");
            ee.printStackTrace(System.err);
            return;
        }

        System.out.println("Got new Request:");
        System.out.println("From: " + fromLat + ", " + fromLon);
        System.out.println("to: " + toLat + ", " + toLon);

        PointList route = routing(fromLat, fromLon, toLat, toLon);

        JSONObject response = new JSONObject();
        response.put("uuid", uuid);
        JSONArray jsa = new JSONArray();

        for(GHPoint3D point : route){
            JSONObject jso = new JSONObject();
            jso.put("lat", point.lat);
            jso.put("lon", point.lon);
            jsa.put(jso);
        }

        System.out.println("Response send to: " + Entry.mqttPrefix + Entry.mqttResponseTopic + "/" + uuid);
        response.put("route", jsa);

        MqttMessage responseMessage = new MqttMessage(response.toString().getBytes());

        _client.publish(Entry.mqttPrefix + Entry.mqttResponseTopic + "/" + uuid, responseMessage);

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        if(Objects.equals(topic, Entry.mqttRequestTopic)){
            routeRequest(mqttMessage);
        }
    }
}
