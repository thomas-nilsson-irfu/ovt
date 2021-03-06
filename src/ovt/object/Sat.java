/*=========================================================================

 Program:   Orbit Visualization Tool
 Source:    $Source: /stor/devel/ovt2g/ovt/object/Sat.java,v $
 Date:      $Date: 2006/03/21 12:17:21 $
 Version:   $Revision: 2.14 $


 Copyright (c) 2000-2003 OVT Team (Kristof Stasiewicz, Mykola Khotyaintsev, 
 Yuri Khotyaintsev)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification is permitted provided that the following conditions are met:

 * No part of the software can be included in any commercial package without
 written consent from the OVT team.

 * Redistributions of the source or binary code must retain the above
 copyright notice, this list of conditions and the following disclaimer.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS
 IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT OR
 INDIRECT DAMAGES  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE.

 OVT Team (https://ovt.irfu.se)   K. Stasiewicz, M. Khotyaintsev, Y.
 Khotyaintsev

 =========================================================================*/
package ovt.object;

import ovt.*;
import ovt.interfaces.*;
import ovt.datatype.*;
import ovt.gui.*;
import ovt.util.*;
import ovt.event.*;
import ovt.model.bowshock.*;

import java.io.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Represents a satellite node in the GUI tree.
 */
public abstract class Sat extends VisualObject implements CoordinateSystemChangeListener,
        TimeChangeListener, MagPropsChangeListener, PositionSource, MenuItemsSource {

    protected File orbitFile;

    /**
     * Satellite's trajectory
     */
    protected Trajectory tra = new Trajectory();
    double maxVelocity = 0, minVelocity = Double.MAX_VALUE;

    //added by kono:
    protected Tle tleFile;

    protected String spinFileName;
    protected SpinData spinData;

    protected SatelliteModule satelliteModule;
    protected OrbitModule orbitModule;
    protected LabelsModule labelsModule;
    protected MainFieldlineModule mainFieldlineModule;
    protected MagFootprintModule magFootprintModule;
    protected MagTangentModule magTangentModule;
    protected OrbitMonitorModule orbitMonitorModule;

    /**
     * Holds DataModule's
     */
    protected Vector dataModules = new Vector();

    /**
     * Holds value of property firstDataMjd.
     */
    private double firstDataMjd;

    /**
     * Holds value of property lastDataMjd.
     */
    private double lastDataMjd;

    /**
     * Satellite's revolution period [days].
     */
    private double revolutionPeriod = -1;

    /**
     * The catalog satellite number
     */
    private int satNumber = -1;


    /**
     * Contructor. Also used by xml
     */
    public Sat(OVTCore core) {
        super(core, "NonameSat", true); // true = containsVisualChildren
        initialize();
    }

    /*
     public Sat(String name, String orbitFile, OVTCore core)
     throws IOException {
     super(core, name, true);
     Log.log("Sat '" + name + "' :: init ...", 3);
     initialize();
     setOrbitFile(new File(orbitFile));
     }*/

    /**
     * Used by each constructor.
     */
    protected void initialize() {

        try {
            setIcon(new ImageIcon(Utils.findResource("images/satellite.gif")));
        } catch (java.io.FileNotFoundException e2) {
            e2.printStackTrace(System.err);
        }

        // Create Sat modules and put them to children.
        satelliteModule = new SatelliteModule(this);
        addPropertyChangeListener(satelliteModule);
        addChild(satelliteModule);

        orbitModule = new OrbitModule(this);
        addPropertyChangeListener(orbitModule);
        addChild(orbitModule);

        labelsModule = new LabelsModule(this);
        addPropertyChangeListener(labelsModule);
        addChild(labelsModule);

        mainFieldlineModule = new MainFieldlineModule(this);
        addPropertyChangeListener(mainFieldlineModule);
        addChild(mainFieldlineModule);

        // create mainFootprintModule
        AbstractSatModule mainFootprintModule = new AbstractSatModule(this, "Footprints", "images/footprints.gif", true);
        // create magFootprintModule
        magFootprintModule = new MagFootprintModule(mainFootprintModule);
        // let MagFPM listen fo eventd from mainFootprintModule
        mainFootprintModule.addPropertyChangeListener(magFootprintModule);
        mainFootprintModule.addChild(magFootprintModule);
        // add mainFootprintModule
        addPropertyChangeListener(mainFootprintModule);
        addChild(mainFootprintModule);

        orbitMonitorModule = new OrbitMonitorModule(this);
        addPropertyChangeListener(orbitMonitorModule);
        addChild(orbitMonitorModule);

        magTangentModule = new MagTangentModule(this);
        addPropertyChangeListener(magTangentModule);
        addChild(magTangentModule);

    }


    public File getOrbitFile() {
        return orbitFile;
    }


    /**
     * Sets orbitFile, firstMjd, lastMjd, revolutionPeriod and spinData. If a
     * file is given, then it assumes that there is a corresponding spin file
     * with other suffix, ".spin".
     *
     * NOTE: It appears that class "Settings" calls this method with a File
     * object initialized with the path "null" (a string!) when loading settings
     * which once originated from a null pointer returned from getOrbitFile.
     * Therefore the method treats the path "null" (the string) the same as the null
     * pointer.
     *
     * @param orbitFile Null means there is no file (i.e. another data source is
     * used).
     */
    public void setOrbitFile(File orbitFile) throws IOException {

        if ((orbitFile != null) && (!orbitFile.getPath().equals("null"))) {
            if (!orbitFile.exists()) {
                throw new IOException("File " + orbitFile + " does not exist");
            }
            if (orbitFile.isDirectory()) {
                throw new IOException("File " + orbitFile + " is a directory");
            }
            this.orbitFile = orbitFile; // set it first!! 

            // Set spin data if spin file is available
            String tmps = orbitFile.getAbsolutePath();
            int tmpi = tmps.lastIndexOf('.');
            this.spinFileName = tmps.substring(0, tmpi) + ".spin";
            this.spinData = new SpinData(spinFileName);
        } else {
            this.orbitFile = null;
            this.spinFileName = null;
            this.spinData = new SpinData(null);
        }
        final double firstLastMjdPeriodSatNumber[] = getFirstLastMjdPeriodSatNumber();
        firstDataMjd = firstLastMjdPeriodSatNumber[0];
        lastDataMjd = firstLastMjdPeriodSatNumber[1];
        revolutionPeriod = firstLastMjdPeriodSatNumber[2];
        satNumber = (int) firstLastMjdPeriodSatNumber[3];

        updateEnabled();
    }


    /**
     * Return the catalog satellite number
     */
    public int getSatNumber() {
        return satNumber;
    }


    /**
     * Returns the time for the first and last orbital data in the orbit data
     * file, period of the satellite revolution. All inherited classes should
     * implement this method. Preferably private.
     *
     * @return res[0] - firstMjd, res[1] - lastMjd, res[2] - period [days],
     * res[3] - s/c catalog number (int)
     */
    abstract double[] getFirstLastMjdPeriodSatNumber() throws IOException;


    /**
     * Returns the time for the first data entry in OrbitFile
     *
     * @return The time for the first data in OrbitFile
     */
    public final double getFirstDataMjd() {
        return firstDataMjd;
    }


    /**
     * Returns the time for the last data entry in OrbitFile
     *
     * @return Value of property lastDataMjd.
     */
    public final double getLastDataMjd() {
        return lastDataMjd;
    }


    /**
     * Returns orbital period. Unit: days.
     */
    public final double getOrbitalPeriodDays() {
        return revolutionPeriod;
    }


    /**
     * aa
     *
     * @return Value of property timeSet...
     */
    public TimeSet getTimeSet() {
        return getCore().getTimeSettings().getTimeSet();
    }


    /**
     * Getter for property startMjd.
     *
     * @return Value of property startMjd.
     */
    public double getStartMjd() {
        return getTimeSet().getStartMjd();
    }


    /**
     * Getter for property stopMjd.
     *
     * @return Value of property stopMjd.
     */
    public double getStopMjd() {
        return getTimeSet().getStopMjd();
    }


    /**
     * Getter for property stepMjd.
     *
     * @return Value of property stepMjd.
     */
    public double getStepMjd() {
        return getTimeSet().getStepMjd();
    }


    protected void updateTrajectory() {
        System.out.println("Computing orbit for " + getName() + " ... ");

        tra.clear();
        //System.out.println(new TimeSet(getStartMjd(), getStopMjd(), getStepMjd()));

        double[] timeMjdMap = getTimeSet().getValues();
        long start = System.currentTimeMillis();
        int N = timeMjdMap.length;

        final double[][] gei_arr = new double[N][3];
        final double[][] vei_arr = new double[N][3];

        try {
            fill_GEI_VEI(timeMjdMap, gei_arr, vei_arr);
        } catch (IOException e2) {
            getCore().sendErrorMessage("Error computing " + getName() + "'s orbit.", e2);
            /* NOTE: Failing to retrieve the trajectory still leads to OVT using
             the default variable values instead, i.e. zero. It also means that OVT
             thinks it does have a valid trajectory for the current time interval until
             the current time interval is changed and a new trajectory data is requested.
             This is kind of (maybe) a problem for trajectories which are downloaded
             from the internet and where the code might fail on the first attempt
             but succeed on a later attempt. This is unintuitive behaviour for the user. */
        }
        long fill_time = System.currentTimeMillis();
        Log.log("fill_GEI_VEI: " + (fill_time - start) / 1000.0 + " seconds", 3);

        // Put orbit data in TrajectoryPoint objects ("trp") which in turn
        // are put in a Trajectory object ("tra").
        for (int k = 0; k < N; k++) {
            final double mjd = timeMjdMap[k];
            final TrajectoryPoint trp = new TrajectoryPoint();
            trp.mjd = mjd;

            for (int i = 0; i < 3; i++) {
                trp.gei[i] = gei_arr[k][i] / Const.RE;   // NOTE: Changes units: km --> Earth radii.
                trp.vei[i] = vei_arr[k][i];         // NOTE: Does NOT change units.
            }

            final double velocity = Vect.absv(trp.vei);
            if (velocity > maxVelocity) {
                maxVelocity = velocity;
            }
            if (velocity < minVelocity) {
                minVelocity = velocity;
            }

            // Calculate coordinate values for other coordinate systems than GEI.
            final Trans trans = getTrans(mjd);
            trp.geo = trans.gei2geo(trp.gei);  // Transform gei to geo        
            trp.gsm = trans.geo2gsm(trp.geo);  // Transform geo to gsm        
            trp.gse = trans.gei2gse(trp.gei);  // Transform gei to gse        
            trp.sm = trans.gsm2sm(trp.gsm);    // Transform again.. .-)
            

            tra.put(trp);
        }

        final long endtime = System.currentTimeMillis();
        Log.log("Coordinate transformation time: " + (endtime - fill_time) / 1000.0 + " seconds", 3);

        System.out.println(getName() + "'s trajectory has " + tra.size() + " points.");

        // System.out.println("max mlat = " + ClatMax + "\n");
        valid = true;
    }


    /**
     * This method should fill in gei_arr and vei_arr with the positions and
     * velocities of the satellite (based on the orbit file if there is one).
     *
     * NOTE: Empirically it appears that preceeding calls to updateEnabled make
     * sure that this function is never called for times outside the valid time
     * range (firstDataMjd & lastDataMjd) for which there is data, but the
     * caller does not check for data gaps (and can not easily be made to).
     *
     * @param geiarr Array in which the method puts satellite positions. The
     * first index labels the coordinate tuple (the position). The second index
     * represents the coordinate axis (x,y,z). [position][X/Y/Z]. Unit: km<BR>
     *
     * @param veiarr Array in which the method puts satellite velocities. Unit:
     * km/s according to TrajectoryPoint where the value ends up. NOTE: It is
     * possible that is information is only used for better interpolation.
     *
     * @param timeMjdMap Points in times for which the method should return
     * positions.
     */
    abstract void fill_GEI_VEI(double[] timeMjdMap, double[][] gei_arr, double[][] vei_arr) throws IOException;


    /**
     * Returns Sat's Trajectory
     */
    public Trajectory getTrajectory() {
        if (!isValid()) {
            validate();
        }
        return tra;
    }


    /**
     * Returns current Sat's TrajectoryPoint
     */
    public TrajectoryPoint getTrajectoryPoint() {
        return getTrajectory().get(getMjd());       // here returned null!!!
    }


    /**
     * Returns Sat's position in current CS
     */
    public double[] getPosition() {
        TrajectoryPoint p = getTrajectoryPoint();   // here returned null!!!
        if (p != null) {
            return p.get(getCS());
        } else {
            return null;
        }
    }


    /**
     * Returns Sat's position in GSM CS
     */
    public double[] getPositionGSM() {
        TrajectoryPoint p = getTrajectoryPoint();   // here returned null!!!
        if (p != null) {
            return p.get(CoordinateSystem.GSM);
        } else {
            return null;
        }
    }


    /**
     * Returns interpolated Sat's position based on trajectory object in current
     * CS for specified mjd.
     */
    public double[] getPosition(double mjd) {
        return getTrajectory().evaluate(mjd, getCS());
    }


    public double[] getOrbitPlaneNormal(double mjd) {
        return getTrajectory().getOrbitPlaneNormal(mjd, getCS());
    }


    /**
     * Returns the vector which lies in the orbit plane and perpendicular to
     * trajectory.
     */
    public double[] getTrajectoryNormal(double mjd) {
        return getTrajectory().getNormal(mjd, getCS());
    }


    public FootprintCollection[] getMagFootprintCollection() {
        return magFootprintModule.getFootprintCollection();
    }


    public MagPoint[] getMagFootprints(double mjd) {
        FootprintCollection[] fpc = getMagFootprintCollection();
        return new MagPoint[]{fpc[0].get(mjd), fpc[1].get(mjd)};
    }


    public final boolean canBeVisible() {
        return (getTrajectoryPoint() != null);
    }


    /**
     * Returns true if the sat is in solar wind
     */
    public boolean isInSolarWind() {
        double swp = getMagProps().getSWP(getMjd());
        double machNumber = getMagProps().getMachNumber(getMjd());
        return Bowshock99Model.isInSolarWind(getPositionGSM(), swp, machNumber);
    }


    //-----------------  FieldLine stuff------------
    protected FieldlineCollection getFieldlineCollection(int type) {
        return mainFieldlineModule.getFieldlineCollection(type);
    }


    protected Fieldline getFieldline(int type, double mjd) {
        return mainFieldlineModule.getFieldline(type, mjd);
    }


    public void validate() {
        updateTrajectory();
        valid = true;
    }

// ----------------- Spin stuff -----------------------  

    /**
     * @return spin vector in gei
     */
    protected double[] getSpinVectGEI(double mjd) {
        return spinData.getSpinVect(mjd);
    }


    //added by kono
    /**
     * @return null or spin vector in current CS (abs. value of vector is a spin
     * rate)
     */
    public double[] getSpinVectorRate() {
        if (this.isSpinAvailable()) {
            double[] geiSpin = getSpinVectGEI(getMjd());
            if (geiSpin == null) {
                return null;
            }
            Trans trans = this.getTrans(getMjd());
            Matrix3x3 mtrx = trans.gei_gsm_trans_matrix(); //GEI -> GSM
            double[] gsmSpin = mtrx.multiply(geiSpin);
            mtrx = trans.gsm_trans_matrix(getCS());        //GSM -> current CS
            return mtrx.multiply(gsmSpin);               // result in current CS
        } else {
            return null;
        }
    }


    /**
     *
     * @return null or angles between spin & B(mag. field), V(velocity), S(sun)
     * vectors in radians
     */
    public double[] getSpinAngles() {
        double mjdx = getMjd();
        double[] geiSpinVect = getSpinVectGEI(mjdx);  //spin vect in GEI
        if (geiSpinVect == null) {
            return null;
        }
        /*GeiAndVei geivei;
         try {
         geivei=this.getSatPos(mjdx);                   //sat pos&vel in GEI CS
         } catch (IOException e){
         return null;
         }*/
        TrajectoryPoint trp = getTrajectoryPoint();
        Trans trans = this.getTrans(mjdx);
        Matrix3x3 mtrx = trans.gei_gsm_trans_matrix();      //GEI -> GSM matrix

        double[] gsmSpinVect = mtrx.multiply(geiSpinVect);  //spin vect in GSM
        double[] gsmPos = mtrx.multiply(trp.gei);        //sat position in GSM
        double[] gsmVel = mtrx.multiply(trp.vei);        //sat velocity in GSM
        double[] gsmB = this.getMagProps().bv(gsmPos, mjdx); // B vector in GSM
        double[] geiES = Utils.sunmjd(mjdx);                //Earth->Sun vect in GEI
        double[] gsmES = mtrx.multiply(geiES);              //Earth->Sun vect in GSM
        double[] gsmSS = Vect.sub(gsmES, gsmPos);            //Sat->Sun vect in GSM

        Vect.normf(gsmB, 1.0);
        Vect.normf(gsmSS, 1.0);
        Vect.normf(gsmVel, 1.0);
        Vect.normf(gsmSpinVect, 1.0);

        double[] angles = new double[3];
        angles[0] = Vect.angleOf2vect(gsmSpinVect, gsmB);   //angle between Sp & B
        angles[1] = Vect.angleOf2vect(gsmSpinVect, gsmVel); //angle between Sp & V
        angles[2] = Vect.angleOf2vect(gsmSpinVect, gsmSS);  //angle between Sp & SunDir

        return angles;  //all in radians!!!, kono
    }


    //added by kono
    public boolean isSpinAvailable() {
        return spinData.isAvailable();
    }

// ------------ Bean Properties for XML ----------------

    public SatelliteModule getSatelliteModule() {
        return satelliteModule;
    }


    public OrbitModule getOrbitModule() {
        return orbitModule;
    }


    public LabelsModule getLabelsModule() {
        return labelsModule;
    }


    public MainFieldlineModule getMainFieldlineModule() {
        return mainFieldlineModule;
    }


    public MagFootprintModule getMagFootprintModule() {
        return magFootprintModule;
    }


    public OrbitMonitorModule getOrbitMonitorModule() {
        return orbitMonitorModule;
    }


    public MagTangentModule getMagTangentModule() {
        return magTangentModule;
    }


    public void timeChanged(TimeEvent evt) {

        if (evt.timeSetChanged()) {
            // current time and time period have changed
            updateEnabled();
            invalidate();
        }

        // Tell children about time change
        Enumeration e = getChildren().elements();
        while (e.hasMoreElements()) {
            try {
                ((TimeChangeListener) (e.nextElement())).timeChanged(evt);
            } catch (ClassCastException e2) {
                System.out.println("this module doesn't care about time..");
            }
        }
    }


    public void updateEnabled() {
        TimeSet timeSet = getCore().getTimeSettings().getTimeSet();
        double globalStart = timeSet.getStartMjd();
        double globalStop = timeSet.getStopMjd();

        /* Check whether the global time period is _entirely_ covered by the Sat's
         available data period. If not, setEnabled(false).
         ==> The satellite will be unchecked in the tree panel and the orbit will not be shown.
         NOTE: This feature can not check for data gaps.
         */
        if (getFirstDataMjd() <= globalStart && getLastDataMjd() >= globalStop) {
            if (!isEnabled()) {
                setEnabled(true);
            }
        } else {
            // torbets
            if (isEnabled()) {
                setEnabled(false);
            }
        }
    }


    @Override
    public void coordinateSystemChanged(CoordinateSystemEvent evt) {
        Enumeration e = getChildren().elements();
        while (e.hasMoreElements()) {
            try {
                ((CoordinateSystemChangeListener) (e.nextElement())).coordinateSystemChanged(evt);
            } catch (ClassCastException e2) {
                //System.out.println("this module doesn't care about cs..");
            }
        }
    }


    @Override
    public void magPropsChanged(MagPropsEvent evt) {
        mainFieldlineModule.magPropsChanged(evt);
        magFootprintModule.magPropsChanged(evt);
        magTangentModule.magPropsChanged(evt);
        if (!OVTCore.isServer()) {
            orbitMonitorModule.magPropsChanged(evt);
        }
    }


    @Override
    public JMenuItem[] getMenuItems() {
        JMenuItem item0 = new JMenuItem("Info");
        item0.setFont(Style.getMenuFont());
        item0.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SatInfoWindow infoWindow = new SatInfoWindow(getCore().getXYZWin());
                infoWindow.setObject(Sat.this);
                infoWindow.setVisible(true);
            }
        });

        JMenuItem item1 = new JMenuItem("Orbit Monitor");
        item1.setFont(Style.getMenuFont());
        item1.setEnabled(isEnabled());
        item1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                orbitMonitorModule.setVisible(true);
            }
        });
        JMenuItem item2 = new JMenuItem("Load data...");
        item2.setFont(Style.getMenuFont());
        item2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                LoadDataWizard wizard = new LoadDataWizard(Sat.this, getCore().getXYZWin());
                DataModule data = wizard.start();
                if (data != null) {
                    addDataModule(data);
                    getChildren().fireChildAdded(data);
                }
            }
        });
        JMenuItem item3 = new JMenuItem("Remove");
        item3.setFont(Style.getMenuFont());
        item3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getCore().getSats().removeSatAction(Sat.this);
//                getCore().getSats().removeSat(Sat.this);
//                getCore().getSats().getChildren().fireChildRemoved(Sat.this); // for TreePanel
            }
        });
        return new JMenuItem[]{item0, null, item1, null, item2, null, item3};
    }


    public void dispose() {
        setVisible(false);
        super.dispose(); // dispose descriptors, remove listeners, dispose children
    }


    public void addDataModule(DataModule module) {
        dataModules.addElement(module);
        addChild(module);
    }


    public void removeDataModule(DataModule module) {
        module.dispose();
        dataModules.removeElement(module);
        getChildren().removeChild(module);
    }


    public DataModule[] getDataModules() {
        Object[] objArray = dataModules.toArray();
        DataModule[] res = new DataModule[objArray.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (DataModule) objArray[i];
        }
        return res;
    }


    public void setDataModules(DataModule[] modules) {
        // remove old DataModel's
        Enumeration e = dataModules.elements();
        while (e.hasMoreElements()) {
            removeDataModule((DataModule) e.nextElement());
        }
        // add new
        for (int i = 0; i < modules.length; i++) {
            addDataModule(modules[i]);
        }
    }

}
