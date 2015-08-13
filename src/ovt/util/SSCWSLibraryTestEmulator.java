/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovt.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import ovt.Const;
import ovt.datatype.Time;

/**
 * Test code
 *
 * Class which instances can replace SSC Web Services (SSCWSLibraryImpl) as a
 * source of data for testing purposes. This class is supposed to return
 * "better" FICTIOUS test data for the purposes of testing the entire GUI:<BR>
 * - Graphics (3D visualizations)<BR>
 * - Orbital period calculations<BR>
 * - Orbit data resolution (possibly varying over time) and interpolation<BR>
 * - Menus (selecting satellites, tree panel)<BR>
 * - Caching, download delays<BR>
 * - Data gaps<BR>
 * - Various errors, error messages: network failures, exceptions etc.<BR>
 *
 * NOTE: One can throw exceptions to simulate network and SSC Web Services
 * failure. NOTE: One could add delays to simulate download over internet.
 *
 * Should therefore return somewhat physical orbits, at least by length scales
 * and time scales (velocities).
 *
 * @author Erik P G Johansson, erik.johansson@irfu.se
 */
public class SSCWSLibraryTestEmulator extends SSCWSLibrary {

    private static final int DEBUG = 1;   // Set the minimum log message level for this class.
    public static final SSCWSLibrary DEFAULT_INSTANCE = new SSCWSLibraryTestEmulator();

    private static final int DATAGAPSAT_PERIOD = 2;
    /**
     * Numbers (labels) the current call for data for DownloadFailSat. First
     * call is "1" (sic!).
     */
    private int downloadFailSatRequests = 0;
    private static final int DOWNLOADFAILSAT_FAIL_PERIOD = 2;
    /**
     * Determines which DownloadFailSat download request should be the first to
     * fail. Primarily used to prevent automatic initialization (orbit period
     * calculation) from failing.
     */
    private static final int DOWNLOADFAILSAT_FIRST_FAIL = 2;

    final double COMP_ORBIT_SAT_2A_PERTURBATION_RMS_KM = 1.0;   // RMS for every axis separately.
    final double COMP_ORBIT_SAT_2B_TIME_SHIFT_MJD = Time.DAYS_IN_SECOND * 1.0;

    final int N_GENERIC_SATELLITES = 100;

    private final double dataBeginMjd = Time.getMjd(1950, 1, 1, 0, 0, 1);
    private final double dataEndMjd = Time.getMjd(2300, 1, 1, 0, 0, 0);


    private SSCWSLibraryTestEmulator() {
    }


    @Override
    public List<SSCWSSatelliteInfo> getAllSatelliteInfo() {
        final List<SSCWSSatelliteInfo> satInfos = new ArrayList<>();
        final int bestTimeResolution = 60;

        satInfos.add(new SSCWSSatelliteInfo("Enterprise", "USS Enterprise (NCC-1701)", dataBeginMjd, dataEndMjd, bestTimeResolution));
        satInfos.add(new SSCWSSatelliteInfo("UFO", "Unidentified Flying Object", dataBeginMjd, dataEndMjd, bestTimeResolution));

        satInfos.add(new SSCWSSatelliteInfo("DataGapSat", "Satellite_w_data_gaps_for_even_mjd", dataBeginMjd, dataEndMjd, bestTimeResolution));
        satInfos.add(new SSCWSSatelliteInfo("DownloadFailSat", "Satellite_w_download_failures", dataBeginMjd, dataEndMjd, bestTimeResolution));

        satInfos.add(new SSCWSSatelliteInfo("CompOrbitSat1", "Compare-orbit Sat 1", dataBeginMjd, dataEndMjd, bestTimeResolution));
        satInfos.add(new SSCWSSatelliteInfo("CompOrbitSat2a", "Compare-orbit Sat 2a w perturb", dataBeginMjd, dataEndMjd, bestTimeResolution));
        satInfos.add(new SSCWSSatelliteInfo("CompOrbitSat2b", "Compare-orbit Sat 2b w time delay", dataBeginMjd, dataEndMjd, bestTimeResolution));
        //satInfos.add(new SSCWSSatelliteInfo("SlowDownloadSat", "Satellite_w_simulated_slow_downloads", dataBeginMjd, dataEndMjd, bestTimeResolution));

        for (int i = 0; i < N_GENERIC_SATELLITES; i++) {
            satInfos.add(new SSCWSSatelliteInfo("ZzzzSat" + i, "Zome Generic Satellite " + i, dataBeginMjd + i, dataEndMjd - i, bestTimeResolution));
        }
        return Collections.unmodifiableList(satInfos);
    }


    @Override
    /**
     * NOTE: Returns lengths in km.
     *
     * @return 2D array of 4D coordinates, [X/Y/Z/time][position] in km & mjd.
     */
    public double[][] getTrajectory_GEI(
            String satID,
            double beginMjdInclusive, double endMjdInclusive,
            int resolutionFactor)
            throws IOException {

        // Argument check
        if (resolutionFactor <= 0) {
            throw new IllegalArgumentException("Illegal argument resolutionFactor=" + resolutionFactor + ".");
        }
        if (endMjdInclusive <= beginMjdInclusive) {
            throw new IllegalArgumentException("endMjdInclusive <= beginMjdInclusive");
        }

        // Limit arguments to available range.
        beginMjdInclusive = Math.max(beginMjdInclusive, this.dataBeginMjd);
        endMjdInclusive = Math.min(endMjdInclusive, this.dataEndMjd);

        final SSCWSSatelliteInfo satInfo = getSatelliteInfo(satID);

        if (satID.equals("CompOrbitSat1")) {

            return getPhysicallyCorrectCircularOrbit(
                    Utils.getRandomFromString(satID),
                    beginMjdInclusive, endMjdInclusive,
                    satInfo.bestTimeResolution * resolutionFactor);

        } else if (satID.equals("CompOrbitSat2a")) {

            final double[][] coords_axisPos_kmMjd = getTrajectory_GEI(
                    "CompOrbitSat1",
                    beginMjdInclusive,
                    endMjdInclusive,
                    resolutionFactor);                 // NOTE: Recursive call.

            // Modify orbit slightly.
            final Random r = new Random();
            for (int i = 0; i < coords_axisPos_kmMjd[0].length; i++) {
                // NOTE: Unit km
                coords_axisPos_kmMjd[0][i] = coords_axisPos_kmMjd[0][i] + r.nextGaussian() * COMP_ORBIT_SAT_2A_PERTURBATION_RMS_KM;
                coords_axisPos_kmMjd[1][i] = coords_axisPos_kmMjd[1][i] + r.nextGaussian() * COMP_ORBIT_SAT_2A_PERTURBATION_RMS_KM;
                coords_axisPos_kmMjd[2][i] = coords_axisPos_kmMjd[2][i] + r.nextGaussian() * COMP_ORBIT_SAT_2A_PERTURBATION_RMS_KM;
            }
            return coords_axisPos_kmMjd;

        } else if (satID.equals("CompOrbitSat2b")) {

            // NOTE: MINOR BUG: Adding/subtracting from mjd time does not take
            // the time boundaries for available data into account. The check at
            // the beginning of the method clips the data interval.
            final double[][] coords_axisPos_kmMjd = getTrajectory_GEI("CompOrbitSat1",
                    beginMjdInclusive + COMP_ORBIT_SAT_2B_TIME_SHIFT_MJD,
                    endMjdInclusive + COMP_ORBIT_SAT_2B_TIME_SHIFT_MJD,
                    resolutionFactor);                                  // NOTE: Recursive call.
            for (int i = 0; i < coords_axisPos_kmMjd[0].length; i++) {
                // NOTE: Unit km
                coords_axisPos_kmMjd[3][i] = coords_axisPos_kmMjd[3][i] - COMP_ORBIT_SAT_2B_TIME_SHIFT_MJD;
            }
            return coords_axisPos_kmMjd;

        } else {

            /*=====================
             Produce orbital data.
             =====================*/
            final double[][] coords = getPhysicallyCorrectCircularOrbit(
                    Utils.getRandomFromString(satID),
                    beginMjdInclusive, endMjdInclusive,
                    satInfo.bestTimeResolution * resolutionFactor);

            /* Case-by-case treatment of test satellites that use previously produced orbit. */
            if (satID.equals("DataGapSat")) {
                /* NOTE: One both may, and may not, want to have data gaps coincide with cache units boundaries. */

                /* Select orbital data points to keep/remove. */
                final double[] t = coords[3];
                int j = 0;
                for (int i = 0; i < t.length; i++) {
                    if (((int) t[i]) % DATAGAPSAT_PERIOD != 0) {
                        // CASE: Even odd mjd number (when rounded to integer).
                        // ==> Include this data point.
                        for (int k = 0; k < 4; k++) {
                            coords[k][j] = coords[k][i];
                        }
                        j++;
                    }
                }
                for (int k = 0; k < 4; k++) {
                    coords[k] = Utils.selectArrayIntervalMC(coords[k], 0, j);
                }
            } else if (satID.equals("DownloadFailSat")) {
                /* NOTE: This method corresponds to downloading from the internet.
                 Data requests that are entirely satisfied by retrieving data in the cache still work.
                 Note that a request to the cache can generate multiple requests to this method.
                 NOTE: The first request for downloading data is (likely) triggered by the need to
                 estimate the time resolution.
                 */
                /* NOTE: OVT presently does not try to retrieve data for a time
                 interval a second time if the fist time fails and the OVT time interval is not changed.*/
                downloadFailSatRequests++;
                if ((downloadFailSatRequests >= DOWNLOADFAILSAT_FIRST_FAIL)
                        && ((downloadFailSatRequests - DOWNLOADFAILSAT_FIRST_FAIL) % DOWNLOADFAILSAT_FAIL_PERIOD == 0)) {
                    final String msg = "Can not download data from SSC Web Services (DownloadFailSat; Test exception).";
                    Log.log("EXCEPTION: " + msg, DEBUG);
                    throw new IOException(msg);
                }
            }/* else {
             throw new RuntimeException("No orbit data has been defined for satID=\"" + satID + "\".");
             }*/

            return coords;
        }
    }


    @Override
    public List<String> getPrivacyAndImportantNotices() {
        final List<String> list = new ArrayList<>();
        list.add("http://getPrivacyAndImportantNotices.com/demonstrationURL.html");
        return Collections.unmodifiableList(list);
    }


    @Override
    public List<String> getAcknowledgements() {
        final List<String> list = new ArrayList<>();
        list.add("http://getAcknowledgements.com/demonstrationURL.html");
        return Collections.unmodifiableList(list);
    }


    /**
     * Return orbital positions for a time interval of a physically correct
     * circular orbit. The orbit is entirely determined by one caller-supplied
     * arbitrary "random" number. Different values can be used to retrieve data
     * for different orbits. Return data is designed to consistently return the
     * same data points (at the same points in time for a given time resolution)
     * so that caching works.
     *
     * NOTE: Returns lengths in km, times in mjd.
     *
     * @param randValue Value between 0 and 1 used to generate/select the orbit.
     * @param timeResolution Unit: seconds.
     * @return 2D array of 4D coordinates, [X/Y/Z/time][position] in km & mjd.
     */
    private static double[][] getPhysicallyCorrectCircularOrbit(
            Random rand,
            double beginMjdInclusive, double endMjdInclusive,
            int timeResolution) {

        Log.log(SSCWSLibraryTestEmulator.class.getSimpleName() + " getPhysicallyCorrectCircularOrbit("
                + "rand=..."
                + ", beginMjdInclusive=" + beginMjdInclusive
                + ", endMjdInclusive+" + endMjdInclusive + ", ...)", DEBUG);

        // NOTE: Gravitational constant (m^3 * kg^-1 * s^-2; does not use km!).
        final double yzAngle_radians = rand.nextDouble() * Math.PI * 2;
        final double R = (1.1 + 2 * rand.nextDouble()) * Const.RE * Const.METERS_PER_KM;
        //Log.log("getPhysicallyCorrectCircularOrbit: R = " + R, DEBUG);
        final double omega = Math.sqrt(Const.GRAV_CONST * Const.ME / (R * R * R)); // Orbital angular velocity

        /* Derive times for which to produce data.
         -----------------------------------------
         We want these points in time to be consistent over different calls,
         and different specified time intervals.
         NOTE: Should use Utils.double2longSafely since it is not obvious that
         one can convert the ratio to int if one uses great test values.
         */
        final double timeResolutionMjd = Time.DAYS_IN_SECOND * timeResolution;
        final long i_begin = Utils.double2longSafely(Math.ceil(beginMjdInclusive / timeResolutionMjd));
        final long i_end = Utils.double2longSafely(Math.floor(endMjdInclusive / timeResolutionMjd));
        final int N = (int) (i_end - i_begin);
        final double[] timesMjd = new double[N];
        int j_array = 0;
        for (long i = i_begin; i < i_end; i++) {
            timesMjd[j_array] = i * timeResolutionMjd;
            j_array++;
        }

        // Calculate orbit data.
        final double[][] coords_axisPos_kmMjd = new double[4][N];
        coords_axisPos_kmMjd[3] = timesMjd;   // axisPos = Indices [axis][position].
        for (int i = 0; i < N; i++) {
            final double t = timesMjd[i] * Time.SECONDS_IN_DAY;
            // NOTE: Math.cos, Math.sin take angles in radians.
            coords_axisPos_kmMjd[0][i] = R * Math.cos(omega * t) / Const.METERS_PER_KM;
            coords_axisPos_kmMjd[1][i] = R * Math.sin(omega * t) * Math.cos(yzAngle_radians) / Const.METERS_PER_KM;
            coords_axisPos_kmMjd[2][i] = R * Math.sin(omega * t) * Math.sin(yzAngle_radians) / Const.METERS_PER_KM;
        }

        return coords_axisPos_kmMjd;
    }

}
