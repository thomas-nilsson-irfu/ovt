/*=========================================================================

Program:   Orbit Visualization Tool
Source:    $Source: /stor/devel/ovt2g/ovt/beans/editor/StringEditor.java,v $
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

OVT Team (http://ovt.irfu.se)   K. Stasiewicz, M. Khotyaintsev, Y.
Khotyaintsev

=========================================================================*/

/*
 * StringEditor.java
 *
 * Created on June 25, 2002, 6:28 PM
 */

package ovt.beans.editor;

import java.beans.*;

import java.awt.Component;

/**
 *
 * @author  ko
 * @version 
 */
public class StringEditor extends PropertyEditorSupport {

    /** Creates new StringEditor */
    public StringEditor() {
    }
    
    public void setAsText(String text) {
        setValue(text);
    }
    
    public String getAsText() {
        return (String)getValue();
    }
    
    public boolean supportsCustomEditor() {
        return true;
    }
    
    public Component getCustomEditor() {
        return new StringEditorPanel(this);
    }
    
    public StringEditorPanel getStringEditorPanel() {
        return new StringEditorPanel(this);
    }

}
