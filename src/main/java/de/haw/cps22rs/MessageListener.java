package de.haw.cps22rs;

import com.google.gson.Gson;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class Coordinate {
    double Latitude;
    double Longitude;

    public Coordinate(double Lat, double Lon){
        Latitude = Lat;
        Longitude = Lon;
    }

    public Coordinate(GHPoint3D point){
        Latitude = point.lat;
        Longitude = point.lon;
    }
}

class RequestMessage {
    String UUID;
    Coordinate From;
    Coordinate To;
}

class ResponseMessage {
    String UUID;
    Coordinate[] Route;

    public ResponseMessage(String uuid, List<Coordinate> route){
        UUID = uuid;
        Route = new Coordinate[route.size()];
        Route = route.toArray(Route);
    }
}

public class MessageListener implements IMqttMessageListener{
    
    private final GraphHopper _hopper;
    private final MqttClient _client;

    private final Gson gson = new Gson();

    private List<Coordinate> routing(Coordinate from, Coordinate to) {
        // simple configuration of the request object
        GHRequest req = new GHRequest(from.Latitude, from.Longitude, to.Latitude, to.Longitude).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile("car");
        GHResponse rsp = _hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
            throw new RuntimeException(rsp.getErrors().toString());

        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();
        PointList route = path.getPoints();
        return StreamSupport.stream(route.spliterator(), true).map(Coordinate::new).collect(Collectors.toList());
    }


    public MessageListener(GraphHopper hopper, MqttClient client){
        _hopper = hopper;
        _client = client;
    }

    private void routeRequest(MqttMessage mqttMessage) throws MqttException {
        RequestMessage message;
        System.out.println(mqttMessage);
        try {
            message = gson.fromJson(mqttMessage.toString(), RequestMessage.class);
        }
        catch (Exception ee){
            System.err.println("Messages malformed!");
            ee.printStackTrace(System.err);
            return;
        }

        System.out.println("Got new Request:");
        System.out.println("From: " + message.From.Latitude + ", " + message.From.Longitude);
        System.out.println("to: " + message.To.Latitude + ", " + message.From.Longitude);

        List<Coordinate> route = routing(message.From, message.To);

        ResponseMessage responseMessage = new ResponseMessage(message.UUID, route);



        String topic = Entry.mqttPrefix + Entry.mqttResponseTopic + "/by-uuid/" + message.UUID;

        System.out.println("Response send to: " + topic);

        MqttMessage mqttResponse = new MqttMessage(gson.toJson(responseMessage).getBytes());

        _client.publish(topic, mqttResponse);

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        if(Objects.equals(topic, Entry.mqttPrefix + Entry.mqttRequestTopic)){
            routeRequest(mqttMessage);
        }
    }
}
