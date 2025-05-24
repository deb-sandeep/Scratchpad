package com.sandy.scratchpad.evtdoc.repo;

import com.sandy.scratchpad.evtdoc.Event;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EvtRepository {

    @Getter
    private static EvtRepository instance = new EvtRepository() ;
    
    private Map<String, Event> eventMap = new HashMap<>() ;
    
    private EvtRepository(){}

    public Event getEvent( String evtName ) {
        return eventMap.computeIfAbsent( evtName, k -> new Event( evtName ) ) ;
    }
    
    public Collection<Event> getEvents() {
        return eventMap.values()
                       .stream()
                       .sorted( (e1, e2) -> e1.getEventName().compareTo( e2.getEventName() ) )
                       .collect( Collectors.toList()) ;
    }
}
