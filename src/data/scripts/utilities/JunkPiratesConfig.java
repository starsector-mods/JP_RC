/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.utilities;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author paul
 */
public class JunkPiratesConfig {
    private static List<String> spineretteSystemWhitelist = new ArrayList<>();
    private static List<String> spineretteTagWhitelist = new ArrayList<>();
    private static List<String> spineretteSystemBlacklist = new ArrayList<>();
    private static List<String> spineretteTagBlacklist = new ArrayList<>();

    public static List<String> getSpineretteSystemWhitelist() {
        return Collections.unmodifiableList(spineretteSystemWhitelist);
    }

    public static List<String> getSpineretteTagWhitelist() {
        return Collections.unmodifiableList(spineretteTagWhitelist);
    }

    public static List<String> getSpineretteSystemBlacklist() {
        return Collections.unmodifiableList(spineretteSystemBlacklist);
    }

    public static List<String> getSpineretteTagBlacklist() {
        return Collections.unmodifiableList(spineretteTagBlacklist);
    }

    public static void loadJunkPiratesModConfig() {
        try {
            JSONObject junkPiratesWhitelists = Global.getSettings().getMergedJSONForMod("data/config/jpConfig/junk_pirates_Config.json", "junk_pirates_release");

            spineretteSystemWhitelist = jsonArrayToList(junkPiratesWhitelists.optJSONArray("spineretteSystemWhitelist"));
            spineretteTagWhitelist = jsonArrayToList(junkPiratesWhitelists.optJSONArray("spineretteTagWhitelist"));
            spineretteSystemBlacklist = jsonArrayToList(junkPiratesWhitelists.optJSONArray("spineretteSystemBlacklist"));
            spineretteTagBlacklist = jsonArrayToList(junkPiratesWhitelists.optJSONArray("spineretteTagBlacklist"));
            
        } catch (IOException | JSONException ex) {
            System.out.println("JP Config Exception " + ex);
        }
    }

    private static List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    list.add(array.getString(i));
                } catch (JSONException e) {
                    // Ignore malformed strings
                }
            }
        }
        return list;
    }
}
