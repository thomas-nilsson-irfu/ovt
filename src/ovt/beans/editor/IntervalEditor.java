/*=========================================================================

Program:   Orbit Visualization Tool
Source:    $Source: /stor/devel/ovt2g/ovt/beans/editor/IntervalEditor.java,v $
Date:      $Date: 2003/09/28 17:52:36 $
Version:   $Revision: 1.2 $


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

/*
 * IntervalEditor.java
 * 
 * Created on June 21, 2002, 4:13 PM
 */

package ovt.beans.editor;

import ovt.datatype.Interval;
import java.beans.*;
import java.awt.Component;
import ovt.util.Log;

/**
 * NOTE: Not to be confused with ovt.beans.IntervalEditor which seems unused.
 * /Erik P G Johansson 2016-02-04
 *
 * @author  ko
 * @version 
 */
public class IntervalEditor extends PropertyEditorSupport {

    //private static final int DEBUG = 2;
    
    /** Creates new IntervalEditor */
    public IntervalEditor() {
    }
    
    public String getAsText() {
        double mjd = (double) getValue();
        return new Interval(mjd).toString(true);
    }
    
    public void setAsText(String s) throws IllegalArgumentException {
        try {
            final Interval interval = new Interval(s);
            //Log.log(getClass().getSimpleName()+"#setAsText", DEBUG);
            //Log.log("   s = \""+s+"\"", DEBUG);
            setValue(interval.getIntervalInDays());
        } catch (NumberFormatException e2) {
            throw new IllegalArgumentException("Time format : ([_d] [_h] [_m] [_s])");
        } catch (IllegalArgumentException e3) {
            throw new IllegalArgumentException("Time format : ([_d] [_h] [_m] [_s])");
        }
    }
    
    public boolean supportsCustomEditor() {
        return true;
    }
    
    public Component getCustomEditor() {
        return new IntervalEditorPanel(this);
    }
    
}
