package com.sandy.scratchpad.evtdoc;

import com.sandy.scratchpad.evtdoc.internal.EventPoint;
import lombok.Getter;
import lombok.Setter;

public class EventTarget extends EventPoint {
    
    @Getter @Setter private boolean sync = false ;
    @Getter @Setter private int priority = 100 ;
    
    public EventTarget( String pkgName, String clsName, String mthName ) {
        super( pkgName, clsName, mthName ) ;
    }
}
