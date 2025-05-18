package com.sandy.scratchpad.evtdoc.repo;

import com.sandy.scratchpad.evtdoc.Event;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class EvtRepository {

    @Getter
    private static EvtRepository instance = new EvtRepository() ;
    
    private Map<String, Event> eventMap = new HashMap<>() ;
    
    private EvtRepository(){}

    public Event getEvent( String evtName ) {
        return eventMap.computeIfAbsent( evtName, k -> new Event( evtName ) ) ;
    }
}
