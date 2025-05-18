package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static cache for profit data that enables sharing between controllers
 * without direct dependencies.
 */
public class ProfitDataCache {

    // Use ConcurrentHashMap for thread safety
    private static final Map<Integer, Map<String, Double>> yearlyProfitData = new ConcurrentHashMap<>();

    /**
     * Stores profit data for a specific year
     *
     * @param year The year
     * @param monthlyData Map of month names to profit values
     */
    public static void storeProfitData(int year, Map<String, Double> monthlyData) {
        yearlyProfitData.put(year, new HashMap<>(monthlyData));
    }

    /**
     * Retrieves profit data for a specific year
     *
     * @param year The year to retrieve data for
     * @return Map of month names to profit values, or null if no data exists
     */
    public static Map<String, Double> getProfitDataForYear(int year) {
        return yearlyProfitData.get(year);
    }

    /**
     * Checks if profit data exists for a specific year
     *
     * @param year The year to check
     * @return true if data exists, false otherwise
     */
    public static boolean hasProfitData(int year) {
        return yearlyProfitData.containsKey(year) && !yearlyProfitData.get(year).isEmpty();
    }

    /**
     * Clears all cached profit data
     */
    public static void clearCache() {
        yearlyProfitData.clear();
    }

    /**
     * Removes cached profit data for a specific year
     *
     * @param year The year to clear data for
     */
    public static void clearYear(int year) {
        yearlyProfitData.remove(year);
    }
}