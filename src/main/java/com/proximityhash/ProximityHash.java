package com.proximityhash;

import ch.hsr.geohash.GeoHash;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

public class ProximityHash {
    public static void main(String[] args) {
        double latitude=	24.189992;
        double longitude=55.762293;
        double radius=5000;
        int precision=6;
        System.out.println(createGeohash(latitude, longitude, radius, precision, false, 1, 12));
    }

    public static boolean inCircleCheck(double latitude, double longitude, double centre_lat, double centre_lon, double radius) {

        double x_diff = longitude - centre_lon;
        double y_diff = latitude - centre_lat;

        if (Math.pow(x_diff, 2) + Math.pow(y_diff, 2) <= Math.pow(radius, 2)) {
            return true;
        }

        return false;
    }

    public static double[] getCentroid(double latitude, double longitude, double height, double width) {

        double y_cen = latitude + (height / 2);
        double x_cen = longitude + (width / 2);

        return new double[]{x_cen, y_cen};
    }

    public static double[] convertToLatLon(double y, double x, double latitude, double longitude) {

        double pi = 3.14159265359;

        double r_earth = 6371000;

        double lat_diff = (y / r_earth) * (180 / pi);
        double lon_diff = (x / r_earth) * (180 / pi) / Math.cos(latitude * pi / 180);

        double final_lat = latitude + lat_diff;
        double final_lon = longitude + lon_diff;

        return new double[]{final_lat, final_lon};
    }
    public static String createGeohash(double latitude, double longitude, double radius, int precision, boolean georaptorFlag, int minLevel, int maxLevel) {

        double x = 0.0;
        double y = 0.0;

        List<double[]> points = new ArrayList<>();
        Set<String> geohashes = new HashSet<>();

        double[] gridWidth = {5009400.0, 1252300.0, 156500.0, 39100.0, 4900.0, 1200.0, 152.9, 38.2, 4.8, 1.2, 0.149, 0.0370};
        double[] gridHeight = {4992600.0, 624100.0, 156000.0, 19500.0, 4900.0, 609.4, 152.4, 19.0, 4.8, 0.595, 0.149, 0.0199};

        double height = (gridHeight[precision - 1])/2;
        double width = (gridWidth[precision-1])/2;

        int latMoves = (int) Math.ceil(radius / height);
        int lonMoves = (int) Math.ceil(radius / width);

        for (int i = 0; i < latMoves; i++) {

            double tempLat = y + height*i;

            for (int j = 0; j < lonMoves; j++) {

                double tempLon = x + width*j;

                if (inCircleCheck(tempLat, tempLon, y, x, radius)) {

                    double[] centroid = getCentroid(tempLat, tempLon, height, width);
                    double[] latlon1 = convertToLatLon(centroid[1], centroid[0], latitude, longitude);
                    double[] latlon2 = convertToLatLon(centroid[1], -centroid[0], latitude, longitude);
                    double[] latlon3 = convertToLatLon(-centroid[1], centroid[0], latitude, longitude);
                    double[] latlon4 = convertToLatLon(-centroid[1], -centroid[0], latitude, longitude);
                    points.add(latlon1);
                    points.add(latlon2);
                    points.add(latlon3);
                    points.add(latlon4);
                }
            }
        }

        for (double[] point : points) {
            geohashes.add(GeoHash.geoHashStringWithCharacterPrecision(point[0], point[1], precision));
        }

        if (georaptorFlag) {
            List<String> georaptorOut = compress(new ArrayList<>(geohashes), minLevel, maxLevel);
            return String.join(",", georaptorOut);
        }
        else {
            return String.join(",", geohashes);
        }
    }
    public static Set<String> getCombinations(String string) {
        List<String> base32 = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "b", "c", "d", "e", "f", "g", "h", "j", "k", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");
        List<String> combinations = new ArrayList<>();
        for (String i : base32) {
            combinations.add(string + i);
        }
        return (Set<String>) combinations;
    }
    public static List<String> compress(List<String> geohashes, int minlevel, int maxlevel) {
        Set<String> deletegh = new HashSet<>();
        Set<String> final_geohashes = new HashSet<>();
        boolean flag = true;
        int final_geohashes_size = 0;

        // Input size less than 32
        if (geohashes.size() == 0) {
            System.out.println("No geohashes found!");
            return null;
        }

        while (flag == true) {
            final_geohashes.clear();
            deletegh.clear();

            for (String geohash : geohashes) {
                int geohash_length = geohash.length();

                // Compress only if geohash length is greater than the min level
                if (geohash_length >= minlevel) {
                    // Get geohash to generate combinations for
                    String part = geohash.substring(0, geohash_length - 1);

                    // Proceed only if not already processed
                    if (!deletegh.contains(part) && !deletegh.contains(geohash)) {

                        // Generate combinations
                        Set<String> combinations = getCombinations(part);

                        // If all generated combinations exist in the input set
                        if (geohashes.containsAll(combinations)) {
                            // Add part to temporary output
                            final_geohashes.add(part);
                            // Add part to deleted geohash set
                            deletegh.add(part);
                        }
                        // Else add the geohash to the temp out and deleted set
                        else {
                            deletegh.add(geohash);

                            // Forced compression if geohash length is greater than max level after combination check failure
                            if (geohash_length >= maxlevel) {
                                final_geohashes.add(geohash.substring(0, maxlevel));
                            } else {
                                final_geohashes.add(geohash);
                            }
                        }

                        // Break if compressed output size same as the last iteration
                        if (final_geohashes_size == final_geohashes.size()) {
                            flag = false;
                        }
                    }
                }
            }

            final_geohashes_size = final_geohashes.size();
            geohashes.clear();

            // Temp output moved to the primary geohash set
            geohashes.addAll(final_geohashes);
        }

        return geohashes;
    }


}
