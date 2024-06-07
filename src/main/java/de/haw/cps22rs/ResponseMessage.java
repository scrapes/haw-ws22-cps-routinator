package de.haw.cps22rs;

import java.util.List;

class ResponseMessage {
    String UUID;
    Coordinate[] Route;

    public ResponseMessage(String uuid, List<Coordinate> route){
        UUID = uuid;
        Route = new Coordinate[route.size()];
        Route = route.toArray(Route);
    }
}
