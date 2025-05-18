package com.sandy.scratchpad.evtdoc.repo;

import com.sandy.scratchpad.evtdoc.Event;
import com.sandy.scratchpad.evtdoc.EventSource;
import com.sandy.scratchpad.evtdoc.EventTarget;
import lombok.Getter;

import java.util.*;

public class EvtPointRepository {

    @Getter
    private static EvtPointRepository instance = new EvtPointRepository() ;
    
    private Map<String, EventSource> srcMap = new LinkedHashMap<>() ;
    private Map<String, EventTarget> tgtMap = new LinkedHashMap<>() ;
    
    private EvtPointRepository(){}

    private String getKey( String pkgName, String clsName, String mthName ) {
        return pkgName + ":" + clsName + ":" + mthName ;
    }
    
    public EventSource getSource( String pkgName, String clsName, String mthName ) {
        String key = getKey( pkgName, clsName, mthName ) ;
        return srcMap.computeIfAbsent( key, k -> new EventSource( pkgName, clsName, mthName ) ) ;
    }

    public EventTarget getTarget( String pkgName, String clsName, String mthName ) {
        String key = getKey( pkgName, clsName, mthName ) ;
        return tgtMap.computeIfAbsent( key, k -> new EventTarget( pkgName, clsName, mthName ) ) ;
    }
    
    public Collection<EventSource> getSources() {
        return srcMap.values() ;
    }
    
    public Collection<EventTarget> getTargets() {
        return tgtMap.values() ;
    }
}
