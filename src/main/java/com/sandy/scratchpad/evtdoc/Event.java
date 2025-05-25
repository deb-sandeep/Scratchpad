package com.sandy.scratchpad.evtdoc;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Event {

    @Getter private final List<EventSource> eventSources = new ArrayList<>() ;
    
    @Getter private final List<EventTarget> eventTargets = new ArrayList<>() ;
    
    @Getter private final String eventName ;
    
    public Event( String eventName ) {
        this.eventName = eventName ;
    }
    
    public List<EventTarget> getPrioritizedEventTargets() {
        this.eventTargets.sort( (t1, t2) -> t2.getPriority() - t1.getPriority() ) ;
        return this.eventTargets ;
    }
}
