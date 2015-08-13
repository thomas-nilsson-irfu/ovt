/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovt.gui;

import java.util.HashSet;
import java.util.Set;
import ovt.OVTCore;

/**
 * Set of SSCWS satellites that are "bookmarked", i.e. shortlisted to also
 * appear on the Satellites menu (or similar).
 *
 * IMPLEMENTATION NOTE: Stores satellite IDs instead of
 * SSCWSLibrary.SSCWSSatelliteInfo objects since the class probably should have
 * proper implementations of equals, hashCode etc which the latter does not
 * have.
 *
 * NOTE: This class does not check whether the satellite IDs are actually valid.
 * The intention is for the list to store bookmarks also when (1) the satellite
 * IDs can not be found in the current (SSCWSLibrary) satellite list, or when it
 * is not available (network failure). One could imagine that code could check
 * for the validity of the stored satellite IDs but one does not want bookmarks to be
 * removed just because of network failure or because of switching between real
 * SSCWSLibrary and a test emulator one.
 *
 * @author Erik P G Johansson, erik.johansson@irfu.se
 */
public class SSCWSSatellitesBookmarks {

    private final Set<String> bookmarkedSatIds = new HashSet();


    public Set<String> getBookmarkedSSCWSSatIds() {
        return new HashSet(bookmarkedSatIds);   // Return copy (instead of read-only view), in case the information changes.
    }


    public void setBookmark(String satId, boolean addBookmark) {
        if (addBookmark) {
            bookmarkedSatIds.add(satId);
        } else {
            bookmarkedSatIds.remove(satId);
        }
    }


    public boolean isBookmark(String satId) {
        return bookmarkedSatIds.contains(satId);
    }


    /**
     * Sets contents from string (multiple satellites).
     *
     * IMPLEMENTATION NOTE: Handles null value since OVTCore.getGlobalSetting
     * may returs null if it does not find a property.
     * 
     * @param value String as it is stored in global settings, representing the
     * contents of the class. null means empty.
     */
    // PROPOSAL: Better name.
    public void loadFromGlobalSettingsValue(String value) {
        //final String s = OVTCore.getGlobalSetting(SETTINGS_BOOKMARKED_SSCWS_SATELLITE_IDS);
        bookmarkedSatIds.clear();
        if (value == null) {
            return;  // Property was not found.
        }

        // First tried using StringTokenizer but that (the Java library class)
        // seemed to have some form of bug the produced a never-ending loop.
        final String[] satIDs = value.split(";");
        for (String satId : satIDs) {
            bookmarkedSatIds.add(satId);
        }
    }


    /** @returns Satellite IDs as a list. Always returns a string, also when empty (needed for setting global settings).
     */
    public String getGlobalSettingsValue() {
        String list = "";
        boolean first = true;
        for (String satID : bookmarkedSatIds) {
            if (first) {
                list = satID;
                first = false;
            } else {
                list = list + ";" + satID;
            }
        }
        return list;
        //OVTCore.setGlobalSetting(SETTINGS_BOOKMARKED_SSCWS_SATELLITE_IDS, list);
    }

}
