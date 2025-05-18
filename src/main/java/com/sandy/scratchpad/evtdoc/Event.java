package com.sandy.scratchpad.evtdoc;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Event {

    @Getter
    private List<EventSource> eventSources = new ArrayList<>() ;
    
    @Getter
    private List<EventTarget> eventTargets = new ArrayList<>() ;
    
    @Getter
    private String eventName ;
    
    public Event( String eventName ) {
        this.eventName = eventName ;
    }
}
