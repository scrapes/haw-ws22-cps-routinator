package de.haw.cps22rs;

import com.graphhopper.util.shapes.GHPoint3D;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Entry {
    private final static String mapDir = "/maps/";
    private final static String mapName = "hamburg-latest.osm.pbf";
    private final static String mqttHost = "mqttbroker";
    public final static String mqttRequestTopic = "/routinator/request";
    public final static String mqttResponseTopic = "/routinator/response";
    public final static String mqttControlTopic = "/routinator/control";

    public static String mqttPrefix = "";

    public static Boolean running = true;

    public static void main(String[] args) {
        mqttPrefix = System.getenv("ROUTINATOR_PREFIX");
        System.out.println("MQTT Prefix set to: " + mqttPrefix);

        String mapPath = mapDir + mapName;
        GraphHopper hopper = createGraphHopperInstance(mapPath);
        System.out.println("Loaded Graphhopper Instance");
        while(running) {
            try {
                System.out.println("Starting mqttclient");
                runMqttClient(hopper);
            } catch (Exception ee) {
                System.err.println("MQTT throwed");
                ee.printStackTrace(System.err);
            }

            //Wait 5 seconds to reconect
            try {
                //noinspection BusyWait
                Thread.sleep(5000);
            }
            catch (Exception ee){
                ee.printStackTrace(System.err);
            }

        }
        // release resources to properly shutdown or start a new instance
        hopper.close();
    }


    static void runMqttClient(GraphHopper hopper) throws Exception {
        System.out.println("Waiting 10 seconds until server is up");
        Thread.sleep(10000);
        String broker       = "tcp://" + mqttHost + ":1883";
        String clientId     = "Routinator";
        MemoryPersistence persistence = new MemoryPersistence();


        MqttClient mclient = new MqttClient(broker, clientId, persistence);
        MessageListener mListener = new MessageListener(hopper, mclient);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        System.out.println("Connecting to broker: "+broker);
        mclient.connect(connOpts);
        System.out.println("Connected");

        mclient.subscribe(mqttPrefix + mqttRequestTopic, mListener);

        //noinspection StatementWithEmptyBody
        while (mclient.isConnected()) ;

        mclient.close();
    }
    static GraphHopper createGraphHopperInstance(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(ghLoc);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation("target/routing-graph-cache");

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(new Profile("car").setVehicle("car").setWeighting("fastest").setTurnCosts(false));

        // this enables speed mode for the profile we called car
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));

        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        hopper.importOrLoad();
        return hopper;
    }
}
