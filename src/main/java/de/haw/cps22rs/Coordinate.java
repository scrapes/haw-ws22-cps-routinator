package de.haw.cps22rs;
import com.graphhopper.util.shapes.GHPoint3D;

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