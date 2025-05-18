package com.sandy.scratchpad.evtdoc.internal;

import com.sandy.scratchpad.evtdoc.Event;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class EventPoint {

    private String packageName ;
    private String className ;
    private String methodName ;
    private List<Event> events = new ArrayList<>() ;
    
    public EventPoint( String pkgName, String clsName, String mthName ) {
        this.packageName = pkgName ;
        this.className = clsName ;
        this.methodName = mthName ;
    }
}
