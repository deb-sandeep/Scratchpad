package com.sandy.scratchpad.evtdoc.repo;

import com.sandy.scratchpad.evtdoc.Event;
import lombok.Getter;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EvtRepository {

    @Getter
    private final static EvtRepository instance = new EvtRepository() ;
    
    private final Map<String, Event> eventMap = new HashMap<>() ;
    
    private EvtRepository(){}

    public Event getEvent( String evtName ) {
        return eventMap.computeIfAbsent( evtName, k -> new Event( evtName ) ) ;
    }
    
    public Collection<Event> getEvents() {
        return eventMap.values()
                       .stream()
                       .sorted( Comparator.comparing( Event::getEventName ) )
                       .collect( Collectors.toList()) ;
    }
}
