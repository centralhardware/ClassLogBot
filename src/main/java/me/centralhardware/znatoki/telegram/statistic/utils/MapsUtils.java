package me.centralhardware.znatoki.telegram.statistic.utils;

public class MapsUtils {

    public static final String BASE_GOOGLE_MAP_SEARCH_URL = "https://www.google.com/maps/search/";

    /**
     * @param address string with address
     * @return link to google map with giving address
     */
    public static String makeUrl(String address) {
        return BASE_GOOGLE_MAP_SEARCH_URL + address;
    }

}
