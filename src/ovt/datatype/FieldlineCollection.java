/*=========================================================================

  Program:   Orbit Visualization Tool
  Source:    $Source: /stor/devel/ovt2g/ovt/datatype/FieldlineCollection.java,v $
  Date:      $Date: 2003/09/28 17:52:37 $
  Version:   $Revision: 2.3 $


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
 * FieldlineCollection.java
 *
 * Created on March 30, 2000, 6:30 PM
 */
 
package ovt.datatype;

/** 
 *
 * @author  ko
 * @version 
 */
public class FieldlineCollection extends Timetable {

  /** Creates new FieldlineCollection */
  public FieldlineCollection() {
  }
  
  public Fieldline get(double mjd) {
    return (Fieldline)getElement(mjd);
  }
  
  public Fieldline first() {
    return (Fieldline)firstElement();
  }
  
  public Fieldline last() {
    return (Fieldline)lastElement();
  }
  
}
