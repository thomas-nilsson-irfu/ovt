/*=========================================================================

 Program:   Orbit Visualization Tool
 Source:    $Source: /stor/devel/ovt2g/ovt/mag/MagProps.java,v $
 Date:      $Date: 2015/10/14 10:23:00 $
 Version:   $Revision: 2.10 $


 Copyright (c) 2000-2015 OVT Team (Kristof Stasiewicz, Mykola Khotyaintsev, 
 Yuri Khotyaintsev, Erik P. G. Johansson, Fredrik Johansson)
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

 OVT Team (http://ovt.irfu.se)   K. Stasiewicz, M. Khotyaintsev, Y.
 Khotyaintsev, E. P. G. Johansson, F. Johansson

 =========================================================================*/
package ovt.mag;

import ovt.*;
import ovt.beans.*;
import ovt.util.*;
import ovt.event.*;
import ovt.datatype.*;
import ovt.interfaces.*;
import ovt.mag.model.*;
import ovt.object.*;

import java.beans.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;

/**
 * Class contains time INdependent magnetic field properties and references to
 * the (currently) eight time-DEpendent activity indexes
 * (MagActivityEditorDataModel, MagActivityDataEditor).
 *
 * MagProps = Magnetic properties.
 *
 * NOTE: The words "magnetic", "index", and "activity" are somewhat misleading.
 * Not everything referred to is magnetic (e.g. the Mach number) and not all
 * "indexes" are non-dimensional or scalar (e.g. the magnetic field).
 */
public class MagProps extends OVTObject implements MagModel {

    private ovt.OVTCore core;

    /**
     * Minimum absolute value of magnetic field
     */
    public static double BMIN = 3.6;
    /**
     * Maximum absolute value of magnetic field
     */
    public static double BMAX = 63500;
    /**
     * Magnetic moment of the earth for igrf1985 model
     */
    public static final double DIPMOM = -30483.03;

    public static final int NOMODEL = 0;
    public static final int DIPOLE = -10;
    public static final int IGRF = 10;
    public static final int T87 = 87;
    public static final int T89 = 89;
    public static final int T96 = 96;
    public static final int T2001 = 2001;


    /* model = -21.0 ....-25.0      (dipole + tsyganenko 89) <BR>
     *                -11.0 ... -15.0      (dipole + tsyganenko 87)<BR>
     *                -10.0                (dipole)<BR>
     *                 10.0                (igrf)<BR>
     *                 11.0 ...  15.0      (igrf +   tsyganenko 87)<BR>
     *                 21.0 ...  25.0      (igrf + tsyganenko 89)<BR>
     */
//public static double model=-10.0;
    /**
     * arbitrary factor from <code>0.5</code> to <code>1.5</code> Field =
     * internalField + ModelFactor * externalFiels
     *
     * @see ovt.calc.MagPack#magbv(double[])
     */
    public static double modelFactor = 1.0;   // Make private, final, non-static?

    public static double mSub = 11.0;
    public static double bSub = 13.5;

    private static final double xlim_default = -30;

    /**
     * Minimum distance in the tail. Should be negative. Should be made into a
     * private instance variable (i.e. non-static) with a getValues method?
     */
    public double xlim = xlim_default;

    /**
     * Altitude (km) for footprint tracing. Make private?
     */
    public static final double alt = 100;
    /**
     * Altitude (RE) for footprint tracing
     */
    private static final double footprintAltitude = 100. / Const.RE;

    /**
     * Holds (ModelType, Model) pairs. Hashtable is
     */
    protected Hashtable models = new Hashtable();

    /**
     * Utility field used by bound properties. NOTE: Refers to
     * java.beans.PropertyChangeListener, i.e. associated with Java Beans.
     *
     * Appears to be used only for when changing internal and external (magnetic
     * field) model. /Erik P G Johansson 2015-10-09
     */
    private OVTPropertyChangeSupport propertyChangeSupport = new OVTPropertyChangeSupport(this);
    /**
     * Appears to be used only for when "activity" "Apply" buttons are pressed.
     */
    private MagPropsChangeSupport magPropsChangeSupport = new MagPropsChangeSupport(this);

    protected AbstractMagModel internalModel = null;
    protected AbstractMagModel externalModel = null;


    /* Indices representing activity indexes (values that represent some form of quantity in some physical model). */
    public static final int KPINDEX = 1;
    public static final int IMF = 2;
    public static final int SWP = 3;
    public static final int DSTINDEX = 4;
    public static final int MACHNUMBER = 5;
    public static final int SW_VELOCITY = 6;
    public static final int G1 = 7;
    public static final int G2 = 8;
    public static final int MAX_ACTIVITY_INDEX = G2;   // Highest/last index for a activity index. Used for iterating.

    public static final int MAG_FIELD = 30;    // What does this signify? Constant is only used once in OVT.
    public static final int INTERNAL_MODEL = 31;
    public static final int EXTERNAL_MODEL = 32;
    public static final int CLIP_ON_MP = 33;

//public static final int DIPOLE_TILT_COS   = 50;
    public static final int IMF_X = IMF * 100 + 0;
    public static final int IMF_Y = IMF * 100 + 1;
    public static final int IMF_Z = IMF * 100 + 2;

    public static final String KPINDEX_STR = "KPIndex";
    public static final String IMF_STR = "IMF";
    public static final String SWP_STR = "SWP";
    public static final String DSTINDEX_STR = "DSTIndex";
    public static final String MACHNUMBER_STR = "MachNumber";
    public static final String SW_VELOCITY_STR = "SWVelocity";
    public static final String G1_STR = "G1";
    public static final String G2_STR = "G2";

    public static final String MPCLIP = "MP Clipping";  //Added by kono

    /**
     * List of data models tied to the editor window for manually editing tables
     * (or text files) with activity data.
     */
    private final MagActivityEditorDataModel[] activityEditorDataModels = new MagActivityEditorDataModel[MAX_ACTIVITY_INDEX + 1];

    /**
     * List of data sources for "activity" data. The "activity" data that the
     * rest of OVT uses is read from these instances.
     */
    private final MagActivityDataSource[] activityDataSources = new MagActivityDataSource[MAX_ACTIVITY_INDEX + 1];

    /**
     * Editor window for manually editing a table of "activity" values (for one
     * "index") over time.
     */
    private final MagActivityDataEditor[] activityEditors = new MagActivityDataEditor[MAX_ACTIVITY_INDEX + 1];

    public static final double KPINDEX_DEFAULT = 0;
    public static final double[] IMF_DEFAULT = {0, 0, 0};
    public static final double SWP_DEFAULT = 1.8;
    public static final double DSTINDEX_DEFAULT = -40;
    public static final double MACHNUMBER_DEFAULT = 5.4;
    public static final double SW_VELOCITY_DEFAULT = 400;   // Unit: km/s

    private MagPropsCustomizer magPropsCustomizer = null;

    /**
     * Holds value of property customizerVisible.
     */
    private boolean customizerVisible;

    /**
     * Magnetopause clipping.
     */
    private static boolean mpClipping = true;

    /**
     * Holds value of property internalModelType.
     */
    private int internalModelType = IGRF;

    /**
     * Holds value of property externalModelType.
     */
    private int externalModelType = T87;

    /**
     * Select what to use as a raw data source for the functionality/code that
     * handles OMNI2 data. All OMNI2 data should pass through this class. See
     * comments in OMNI2RawDataSource and OMNI2RawDataSourceImpl.
     */
    private static final OMNI2RawDataSource OMNI2_RAW_DATA_SOURCE = new OMNI2RawDataSourceImpl(new File(OVTCore.getUserDir() + OVTCore.getOMNI2CacheSubdir()));

    /**
     * Select what to use as a (non-raw) data source for the functionality/code
     * that handles OMNI2 data. All OMNI2 data should pass through this class.
     * See comments in OMNI2DataSource.
     */
    private static final OMNI2DataSource OMNI2_DATA_SOURCE = new OMNI2DataSource(OMNI2_RAW_DATA_SOURCE);


    /**
     * Creates new magProperties.
     */
    public MagProps(OVTCore core) {
        super("MagModels");
        setParent(core);
        //Log.log("MagProps :: init ...", 3);
        setIcon(new ImageIcon(OVTCore.getImagesSubdir() + "magnet.gif"));
        showInTree(false);
        this.core = core;

        activityEditorDataModels[KPINDEX] = new MagActivityEditorDataModel(KPINDEX, 0, 9, KPINDEX_DEFAULT, "KP Index");
        activityEditorDataModels[IMF] = new MagActivityEditorDataModel(IMF, -50, 50, IMF_DEFAULT, new String[]{"Bx [nT]", "By [nT]", "Bz [nT]"});
        activityEditorDataModels[SWP] = new MagActivityEditorDataModel(SWP, 0, 50, SWP_DEFAULT, "SWP [nPa]");
        activityEditorDataModels[DSTINDEX] = new MagActivityEditorDataModel(DSTINDEX, -500, 50, DSTINDEX_DEFAULT, "DST Index");
        activityEditorDataModels[MACHNUMBER] = new MagActivityEditorDataModel(MACHNUMBER, 1, 15, MACHNUMBER_DEFAULT, "Mach Number");
        activityEditorDataModels[SW_VELOCITY] = new MagActivityEditorDataModel(SW_VELOCITY, 200, 1200, SW_VELOCITY_DEFAULT, "SW Velocity [km/s]");
        activityEditorDataModels[G1] = new MagActivityEditorDataModel(G1, 0, 50, 6, "G1");
        activityEditorDataModels[G2] = new MagActivityEditorDataModel(G2, 0, 50, 10, "G2");

        final DataSourceChoice initialDataSourceChoice = DataSourceChoice.MAG_ACTIVITY_EDITOR;   // TEST
        //final DataSourceChoice initialDataSourceChoice = DataSourceChoice.OMNI2;
        activityDataSources[KPINDEX] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[KPINDEX], KPINDEX_DEFAULT, initialDataSourceChoice);
        activityDataSources[IMF] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[IMF], IMF_DEFAULT, initialDataSourceChoice);
        activityDataSources[SWP] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[SWP], SWP_DEFAULT, initialDataSourceChoice);
        activityDataSources[DSTINDEX] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[DSTINDEX], DSTINDEX_DEFAULT, initialDataSourceChoice);
        activityDataSources[MACHNUMBER] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[MACHNUMBER], MACHNUMBER_DEFAULT, initialDataSourceChoice);
        activityDataSources[SW_VELOCITY] = new ActivityEditorOrOMNI2_DataSource(activityEditorDataModels[SW_VELOCITY], SW_VELOCITY_DEFAULT, initialDataSourceChoice);
        activityDataSources[G1] = activityEditorDataModels[G1];
        activityDataSources[G2] = activityEditorDataModels[G2];

        if (!OVTCore.isServer()) {
            activityEditors[KPINDEX] = new MagActivityDataEditor(activityEditorDataModels[KPINDEX], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[KPINDEX]);
            activityEditors[IMF] = new MagActivityDataEditor(activityEditorDataModels[IMF], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[IMF]);
            activityEditors[SWP] = new MagActivityDataEditor(activityEditorDataModels[SWP], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[SWP]);
            activityEditors[DSTINDEX] = new MagActivityDataEditor(activityEditorDataModels[DSTINDEX], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[DSTINDEX]);
            activityEditors[MACHNUMBER] = new MagActivityDataEditor(activityEditorDataModels[MACHNUMBER], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[MACHNUMBER]);
            activityEditors[SW_VELOCITY] = new MagActivityDataEditor(activityEditorDataModels[SW_VELOCITY], this,
                    (ActivityEditorOrOMNI2_DataSource) activityDataSources[SW_VELOCITY]);
            activityEditors[G1] = new MagActivityDataEditor(activityEditorDataModels[G1], this, null);
            activityEditors[G2] = new MagActivityDataEditor(activityEditorDataModels[G2], this, null);
            magPropsCustomizer = new MagPropsCustomizer(this, getCore().getXYZWin());
            addMagPropsChangeListener(magPropsCustomizer);
        }
        customizerVisible = false;
    }


    public OVTCore getCore() {
        return core;
    }


    public Trans getTrans(double mjd) {
        return getCore().getTrans(mjd);
    }

    /*public String getName(){
     return "ovt.mag.MagProps";
     }*/

    public static double getMaxB() {
        return BMAX;
    }


    public static double getMinB() {
        return BMIN;
    }


    public double getModelFactor() {
        return modelFactor;
    }


    /**
     * Getter for property internalModelType.
     *
     * @return Value of property internalModelType.
     */
    public int getInternalModelType() {
        return internalModelType;
    }


    /**
     * Setter for property internalModelType.
     *
     * @param internalModelType New value of property internalModelType.
     */
    public void setInternalModelType(int internalModelType)
            throws IllegalArgumentException {

        if (internalModelType != NOMODEL && internalModelType != DIPOLE && internalModelType != IGRF) {
            throw new IllegalArgumentException("Invalid internal field model type");
        }

        int oldInternalModelType = this.internalModelType;
        this.internalModelType = internalModelType;
        propertyChangeSupport.firePropertyChange("internalModelType", oldInternalModelType, internalModelType);
    }


    /**
     * Getter for property externalModelType.
     *
     * @return Value of property externalModelType.
     */
    public int getExternalModelType() {
        return externalModelType;
    }


    /**
     * Setter for property externalModelType.
     *
     * @param externalModelType New value of property externalModelType.
     */
    public void setExternalModelType(int externalModelType) {
        System.out.println("Setting external model to " + externalModelType);
        if (externalModelType != NOMODEL && externalModelType != T87
                && externalModelType != T89 && externalModelType != T96 && externalModelType != T2001) {
            throw new IllegalArgumentException("Invalid external field model type");
        }
        if (externalModelType == T2001) { // set xlim to -15 ! This model is not valid for x < -15Re
            xlim = -15;
        } else {
            xlim = xlim_default;
        }
        int oldExternalModelType = this.externalModelType;
        this.externalModelType = externalModelType;
        propertyChangeSupport.firePropertyChange("externalModelType", oldExternalModelType, externalModelType);
    }


    public AbstractMagModel getModel(int modelType) {
        AbstractMagModel model = (AbstractMagModel) models.get(new Integer(modelType));
        if (model == null) {
            // there is no model of modelType
            // create it
            switch (modelType) {
                case NOMODEL:
                    model = new NullModel(this);
                    break;
                case DIPOLE:
                    model = new DipoleModel(this);
                    break;
                case IGRF:
                    model = new IgrfModel(this);
                    break;
                case T87:
                    model = new Tsyganenko87(this);
                    break;
                case T89:
                    model = new Tsyganenko89(this);
                    break;
                case T96:
                    model = new Tsyganenko96(this);
                    break;
                case T2001:
                    model = new Tsyganenko2001(this);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid model type :" + modelType);
            }
            models.put(modelType, model);
        }
        return model;
    }


    public IgrfModel getIgrfModel() {
        return (IgrfModel) getModel(IGRF);
    }

//added by kono

    public void setMPClipping(boolean mode) {
        mpClipping = mode;
    }


    public boolean isMPClipping() {
        return mpClipping;
    }


    // Appears to be unused. /Erik P G Johansson 2015-10-07
    public AbstractMagModel getInternalModel() {
        if (internalModel == null) {
            internalModel = getModel(getInternalModelType());
        }
        return internalModel;
    }


    // Appears to be unused. /Erik P G Johansson 2015-10-07
    public AbstractMagModel getExternalModel() {
        if (externalModel == null) {
            externalModel = getModel(getExternalModelType());
        }
        return externalModel;
    }


    /**
     * Return magnetic field vector.
     *
     * @param gsm Coordinate in GSM
     * @param mjd time
     * @param internalModel internal model type
     * @param externalModel external model type
     * @return Magnetic field vector in nT
     */
    public double[] bv(double[] gsm, double mjd, int internalModel, int externalModel) {
        //Log.log("MagProps.bv(..) executed.",2);
        final double[] result = new double[3];
        final double[] intbv = getModel(internalModel).bv(gsm, mjd);
        final double[] extbv = getModel(externalModel).bv(gsm, mjd);
        for (int i = 0; i < 3; i++) {
            result[i] = intbv[i] + getModelFactor() * extbv[i];
        }
        return result;
    }


    /**
     * Returns magnetic field vector using current internal and external field
     * models.
     *
     * @param gsm point in GSM
     * @param mjd time
     * @return magnetic field vector in nT
     */
    @Override    // Interface MagModel
    public double[] bv(double[] gsm, double mjd) {
        return bv(gsm, mjd, getInternalModelType(), getExternalModelType());
    }


    /**
     * Add a PropertyChangeListener to the listener list.
     *
     * @param l The listener to add.
     */
    @Override
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }


    /**
     * Removes a PropertyChangeListener from the listener list.
     *
     * @param l The listener to remove.
     */
    @Override
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    /**
     * Add a VetoableChangeListener to the listener list.
     *
     * @param l The listener to add.
     */
    @Override
    public void addVetoableChangeListener(java.beans.VetoableChangeListener l) {
        vetoableChangeSupport.addVetoableChangeListener(l);
    }


    /**
     * Removes a VetoableChangeListener from the listener list.
     *
     * @param l The listener to remove.
     */
    @Override
    public void removeVetoableChangeListener(java.beans.VetoableChangeListener l) {
        vetoableChangeSupport.removeVetoableChangeListener(l);
    }


    public void addMagPropsChangeListener(MagPropsChangeListener listener) {
        magPropsChangeSupport.addMagPropsChangeListener(listener);
    }


    public void removeMagPropsChangeListener(MagPropsChangeListener listener) {
        magPropsChangeSupport.removeMagPropsChangeListener(listener);
    }

//----------- DATA ---------------

    public double getKPIndex(double mjd) {
        //Log.log(this.getClass().getSimpleName()+"#getKPIndex("+mjd+"<=>"+new Time(mjd)+")", 2);   // DEBUG
        //final double value = activityEditorDataModels[KPINDEX].getValues(mjd)[0];
        final double value = getActivity(KPINDEX, mjd)[0];
        //Log.log("   value="+value, 2);   // DEBUG
        return value;
    }


    /**
     * Which coordinate system?
     */
    public double[] getIMF(double mjd) {
        return getActivity(IMF, mjd);
    }


    public double getSWP(double mjd) {
        return getActivity(SWP, mjd)[0];
    }


    public double getDSTIndex(double mjd) {
        return getActivity(DSTINDEX, mjd)[0];
    }


    public double getMachNumber(double mjd) {
        //Log.log(this.getClass().getSimpleName() + "#getMachNumber(" + mjd + "<=>" + new Time(mjd) + ")", 2);
        final double value = getActivity(MACHNUMBER, mjd)[0];
        //Log.log("   value=" + value, 2);   // DEBUG
        return value;
    }


    public double getSWVelocity(double mjd) {
        return getActivity(SW_VELOCITY, mjd)[0];
    }


    public double getG1(double mjd) {
        return getActivity(G1, mjd)[0];
    }


    public double getG2(double mjd) {
        return getActivity(G2, mjd)[0];
    }


    public double getSint(double mjd) {
        return getTrans(mjd).getSint();
    }


    public double getCost(double mjd) {
        return getTrans(mjd).getCost();
    }


    /**
     * Returns dipole tilt angle in RADIANS
     *
     * @see #getSint() #getCost()
     */
    public double getDipoleTilt(double mjd) {
        return getTrans(mjd).getDipoleTilt();
    }


    /**
     * Returns footprint altitude (km)
     *
     * @deprecated since 0.001 Use #getFootprintAltitude()
     */
    public double getAlt() {
        return alt;
    }


    /**
     * Returns footprint altitude (RE)
     *
     * @return footprint altitude (RE)
     */
    public double getFootprintAltitude() {
        //System.out.println("getFootprintAlt is Broken.");
        return footprintAltitude;
    }


    public double getXlim() {
        return xlim;
    }


    public Descriptors getDescriptors() {
        if (descriptors == null) {
            // Add default property descriptor for visible property.    
            // each visual object can be hidden or shown.
            try {

                descriptors = new Descriptors();

                BasicPropertyDescriptor pd = new BasicPropertyDescriptor("mPClipping", this);
                pd.setDisplayName("Clipping on Magnetopause");
                pd.setLabel("clip on magnetopause");

                BasicPropertyEditor editor = new CheckBoxPropertyEditor(pd);
                addPropertyChangeListener("mPClipping", editor);
                pd.setPropertyEditor(editor);
                descriptors.put(pd);

                pd = new BasicPropertyDescriptor("internalModelType", this);
                pd.setToolTipText("Internal model");
                pd.setDisplayName("Internal");
                editor = new ComboBoxPropertyEditor(pd, new int[]{DIPOLE, IGRF}, new String[]{"Dipole", "IGRF"});
                addPropertyChangeListener("internalModelType", editor);
                addPropertyChangeListener(editor);
                pd.setPropertyEditor(editor);
                descriptors.put(pd);

                pd = new BasicPropertyDescriptor("externalModelType", this);
                pd.setToolTipText("External model");
                pd.setDisplayName("External");
                editor = new ComboBoxPropertyEditor(pd, new int[]{T87, T89, T96}, new String[]{"Tsyganenko 87", "Tsyganenko 89", "Tsyganenko 96"});
                addPropertyChangeListener("externalModelType", editor);
                addPropertyChangeListener(editor);
                pd.setPropertyEditor(editor);
                descriptors.put(pd);

            } catch (IntrospectionException e2) {
                System.out.println(getClass().getName() + " -> " + e2.toString());
                System.exit(0);
            }
        }
        return descriptors;
    }


    /**
     * Getter for property customizerVisible.
     *
     * @return Value of property customizerVisible.
     */
    public boolean isCustomizerVisible() {
        return customizerVisible;
    }


    /**
     * Setter for property customizerVisible.
     *
     * @param customizerVisible New value of property customizerVisible.
     */
    public void setCustomizerVisible(boolean customizerVisible) {
        magPropsCustomizer.setVisible(customizerVisible);
        this.customizerVisible = customizerVisible;
    }


    public void fireMagPropsChange() {
        magPropsChangeSupport.fireMagPropsChange();
    }


    public void fireMagPropsChange(MagPropsEvent evt) {
        magPropsChangeSupport.fireMagPropsChange(evt);
    }
    /*
     public boolean magFieldConstant(double mjd1, double mjd2) {
     MagActivityEditorDataModel[] indexes = getIndexesFor(getModelTypes());
     for (int i=0; i<indexes.length; i++)
     if (!Vect.equal(indexes[i].getValues(mjd1)), Vect.equal(indexes[i].getValues(mjd2)))
     return false;
     return true;
     }*/


    public static String getActivityName(int index) {
        switch (index) {
            case KPINDEX:
                return KPINDEX_STR;
            case DSTINDEX:
                return DSTINDEX_STR;
            case MACHNUMBER:
                return MACHNUMBER_STR;
            case SWP:
                return SWP_STR;
            case IMF:
                return IMF_STR;
            case SW_VELOCITY:
                return SW_VELOCITY_STR;
            case G1:
                return G1_STR;
            case G2:
                return G2_STR;
        }
        throw new IllegalArgumentException("Illegal index : " + index);
    }

    private double getActivity_cachedMjd = Double.NaN;
    private final Map<Integer, double[]> getActivity_cachedReturnValues = new HashMap();


    /**
     * Get either (1) all values (array) for specific activity index, or (2) one
     * specific value for a specific activity index.
     *
     * NOTE: It appears that all reading of "activity" data by OVT goes through
     * this method. Note that the method is still private. /Erik P G Johansson
     * 2015-10-07.
     *
     * IMPLEMENTATION NOTE: Uses an internal cache to speed up calls.
     * Empirically, we know that the same call is made many times in a row. This
     * is a good place to have a cache since it covers both sources of activity
     * data, MagActivityEditorDataModel and OMNI2.
     *
     * @param key Specify which activity variable that is sought, and optionally
     * which component of that variable. The rule for requesting some component
     * of activity, let's say you need Z component of IMF (IMF[2]).
     * <CODE>key = IMF*100 + 2</CODE>
     * @return Activity values.
     *
     */
    private double[] getActivity(int key, double mjd) {
        //Log.log(this.getClass().getSimpleName()+"#getActivity("+key+", "+mjd+"<=>"+new Time(mjd)+")", 2);

        // Try to use a cached value first.
        // --------------------------------
        // IMPLEMENTATION NOTE: Construct chosen to minimize the number of
        // Integer objects created and the number of calls to Map#containsKey
        // (none) and Map#get.
        if (mjd == getActivity_cachedMjd) {
            final double[] returnValue = getActivity_cachedReturnValues.get(key);
            if (returnValue != null) {
                Log.log(getClass().getSimpleName() + "#getActivity(" + key + ", " + mjd + ") = "
                        + Arrays.toString(returnValue) + "   // Cached value", 2);
                return returnValue;
            }
            // CASE: mjd is the same, but there was no cached value for this particular "key".
            // ==> Keep cached values.
        } else {
            // CASE: mjd has changed.
            // ==> Dismiss all cached values.
            getActivity_cachedMjd = mjd;
            getActivity_cachedReturnValues.clear();
        }

        final double[] returnValue;
        if (key <= 100) {
            returnValue = activityDataSources[key].getValues(mjd);
            //final double[] values = activityDataSources[key].getValues(mjd);
            //Log.log("   double[] values = "+Arrays.toString(values), 2);
            //return values;
        } else {
            final int index = key / 100;
            final int component = key - index * 100;
            //return new double[]{activityDataSources[index].getValues(mjd)[component]};
            returnValue = new double[]{activityDataSources[index].getValues(mjd)[component]};
        }

        getActivity_cachedReturnValues.put(key, returnValue);

        // NOTE: This log value comes AFTER any log value in MagActivityEditorDataModel#getValues.
        Log.log(getClass().getSimpleName() + "#getActivity(" + key + ", " + mjd + ") = "
                + Arrays.toString(returnValue) + "   // (Non-cached value)", 2);
        return returnValue;
    }


    /**
     * @return A set of key - double[] pairs. If some components is requested -
     * INTERNAL_MODEL and EXTERNAL_MODEL are alse valid keys.
     */
    public Characteristics getCharacteristics(int[] keys, double mjd) {
        final Characteristics res = new Characteristics(mjd);
        double[] values = null;
        for (int i = 0; i < keys.length; i++) {
            switch (keys[i]) {
                case INTERNAL_MODEL:
                    values = new double[]{internalModelType};
                    break;
                case EXTERNAL_MODEL:
                    values = new double[]{externalModelType};
                    break;
                case CLIP_ON_MP:
                    values = new double[]{isMPClipping() ? 1 : 0};
                    break;
                default:
                    values = getActivity(keys[i], mjd);
            }
            res.put(keys[i], values);
        }
        return res.getInstance();
    }


    /**
     * @Returns characteristics of magnetic field for mjd.
     *
     */
    public Characteristics getMagFieldCharacteristics(double mjd) {
        int[] keys = null;
        switch (externalModelType) {
            case T87:
                keys = new int[]{KPINDEX, INTERNAL_MODEL, EXTERNAL_MODEL, CLIP_ON_MP};
                break;
            case T89:
                keys = new int[]{KPINDEX, INTERNAL_MODEL, EXTERNAL_MODEL, CLIP_ON_MP};
                break;
            case T96:
                keys = new int[]{IMF_Y, IMF_Z, SWP, DSTINDEX, INTERNAL_MODEL, EXTERNAL_MODEL, CLIP_ON_MP};
                break;
            case T2001:
                keys = new int[]{G1, G2, IMF_Y, IMF_Z, SWP, DSTINDEX, INTERNAL_MODEL, EXTERNAL_MODEL, CLIP_ON_MP};
                break;
        }
        // keys are - data name, magnetic field depends on
        return getCharacteristics(keys, mjd);
    }


    public void setActivityEditorVisible(int index, boolean makeVisible) {
        activityEditors[index].setVisible(makeVisible);
    }


    public void setActivityEditorLocation(int index, int x, int y) {
        activityEditors[index].setLocation(x, y);
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getKPIndexDataModel() {
        return activityEditorDataModels[KPINDEX];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getIMFDataModel() {
        return activityEditorDataModels[IMF];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getSWPDataModel() {
        return activityEditorDataModels[SWP];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getDSTIndexDataModel() {
        return activityEditorDataModels[DSTINDEX];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getMachNumberDataModel() {
        return activityEditorDataModels[MACHNUMBER];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getSWVelocityDataModel() {
        return activityEditorDataModels[SW_VELOCITY];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getG1DataModel() {
        return activityEditorDataModels[G1];
    }


    /**
     * used by XML
     */
    public MagActivityEditorDataModel getG2DataModel() {
        return activityEditorDataModels[G2];
    }

    //##########################################################################
    /**
     * Implementations serve as a data source for "activity" data for a given
     * (fixed) "index".
     */
    public interface MagActivityDataSource {

        /**
         * Returns the relevant "activity" value(s) for an arbitrary point in
         * time. Not that the implementation gets to choose which data point(s)
         * (when in time) to use for the request time.
         */
        public double[] getValues(double mjd);

    }

    public enum DataSourceChoice {

        MAG_ACTIVITY_EDITOR, OMNI2
    }

    /**
     * Class which is a data source for "activity data" and can switch between
     * taking data from (1) a MagActivityEditorDataModel and (2) OMNI2 data.
     * Also serves as data model for the variable/flag (visible in the GUI) that
     * chooses between activityEditorDataMode and OMNI2 data.
     */
    class ActivityEditorOrOMNI2_DataSource implements MagActivityDataSource {

        // TODO: Make sure works with save/load settings (Java Beans).
        private final MagActivityEditorDataModel editorDataModel;
        private final OMNI2Data.FieldID fieldID;
        private final boolean getIMFvector;    // Iff true, then fieldID is irrelevant.
        private final double[] defaultValue;   // Value used/returned in case of error.

        /**
         * Flag for where to take data from.
         */
        private DataSourceChoice dataSourceChoice;


        public ActivityEditorOrOMNI2_DataSource(
                MagActivityEditorDataModel mEditorDataModel,
                double mDefaultValue,
                DataSourceChoice initialDataSourceChoice) {
            this(mEditorDataModel, new double[]{mDefaultValue}, initialDataSourceChoice);
        }


        public ActivityEditorOrOMNI2_DataSource(
                MagActivityEditorDataModel mEditorDataModel,
                double[] mDefaultValue,
                DataSourceChoice initialDataSourceChoice) {

            editorDataModel = mEditorDataModel;
            dataSourceChoice = initialDataSourceChoice;
            defaultValue = mDefaultValue;
            final int activityIndex = editorDataModel.getIndex();

            boolean tempGetIMFvector = false;
            switch (activityIndex) {
                case KPINDEX:
                    fieldID = OMNI2Data.FieldID.Kp;
                    break;
                case IMF:
                    fieldID = OMNI2Data.FieldID.IMFx_nT_GSM_GSE;   // Appropriate value to represent all components of IMF?!!!!
                    tempGetIMFvector = true;
                    break;
                case SWP:
                    fieldID = OMNI2Data.FieldID.SW_ram_pressure_nP;
                    break;
                case DSTINDEX:
                    fieldID = OMNI2Data.FieldID.DST;
                    break;
                case MACHNUMBER:
                    fieldID = OMNI2Data.FieldID.SW_M_ms;
                    break;
                case SW_VELOCITY:
                    fieldID = OMNI2Data.FieldID.SW_velocity_kms;
                    break;
                default:
                    // NOTE: Will yield exception for G1, G2 since these are not in
                    // the OMNI2 data (or at least not read from OMNI2 data.).
                    // /2015-10-14
                    throw new IllegalArgumentException();
            }
            getIMFvector = tempGetIMFvector;
        }


        public void setDataSourceChoice(DataSourceChoice choice) {
            Log.log(getClass().getSimpleName() + "#setDataSourceChoice(" + choice + ")", 2);

            // Avoid calling listeners unnecessarily.
            if (this.dataSourceChoice == choice) {
                return;
            }
            this.dataSourceChoice = choice;

            final MagPropsEvent evt = new MagPropsEvent(this, editorDataModel.getIndex());
            MagProps.this.fireMagPropsChange(evt);
            MagProps.this.getCore().Render();
        }


        public DataSourceChoice getDataSourceChoice() {
            return dataSourceChoice;
        }


        @Override
        public double[] getValues(double mjd) {
            if (dataSourceChoice == DataSourceChoice.MAG_ACTIVITY_EDITOR) {

                return editorDataModel.getValues(mjd);

            } else if (dataSourceChoice == DataSourceChoice.OMNI2) {

                try {
                    return OMNI2_DATA_SOURCE.getValues(mjd, fieldID, getIMFvector);
                } catch (OMNI2DataSource.ValueNotFoundException ex) {

                    final String msg = "Can not find value (" + getActivityName(editorDataModel.getIndex()) + ") for the specified time in the OMNI2 database. Using default value instead. - " + ex.getMessage();
                    getCore().sendWarningMessage("Can not find OMNI2 value", msg);
                    Log.log(msg, 0);
                    return defaultValue;

                } catch (IOException ex) {

                    final String msg = "I/O error when trying to obtain OMNI2 value. Using default value instead. - " + ex.getMessage();
                    getCore().sendErrorMessage("I/O error when obtaining OMNI2 value", msg);
                    Log.log(msg, 0);
                    return defaultValue;

                }

            } else {
                throw new RuntimeException("OVT code bug.");
            }
        }
    }
}

//##############################################################################
/* Could probably be made private.
 * Do not confuse with ovt.beans.MagPropsChangeSupport which is almost identical.
 */
class MagPropsChangeSupport {

    private Vector listeners = new Vector();
    private Object source = null;


    /**
     * Creates new MagPropsChangeSupport
     */
    public MagPropsChangeSupport(Object source) {
        this.source = source;
    }


    public void addMagPropsChangeListener(MagPropsChangeListener listener) {
        listeners.addElement(listener);
    }


    public void removeMagPropsChangeListener(MagPropsChangeListener listener) {
        listeners.removeElement(listener);
    }


    public void fireMagPropsChange(MagPropsEvent evt) {
        Enumeration e = listeners.elements();
        while (e.hasMoreElements()) {
            // NOTE: A separate variable is useful for inspecting variables when debugging.
            final MagPropsChangeListener magPropsChangeListener = (MagPropsChangeListener) e.nextElement();
            magPropsChangeListener.magPropsChanged(evt);
        }
    }


    public void fireMagPropsChange() {
        MagPropsEvent evt = new MagPropsEvent(source, MagProps.MAG_FIELD);
        fireMagPropsChange(evt);
    }


    public boolean hasListener(MagPropsChangeListener listener) {
        return listeners.contains(listener);
    }

}
