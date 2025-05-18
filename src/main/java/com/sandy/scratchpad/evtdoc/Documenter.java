package com.sandy.scratchpad.evtdoc;

import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import org.apache.log4j.Logger;

import java.util.Collection;

public class Documenter {
    
    private static final Logger log = Logger.getLogger( Documenter.class ) ;

    public Documenter() {}
    
    public void generateDocumentation() {
        log.debug( "Generating documentation ..." ) ;
        Collection<EventSource> eventSources = EvtPointRepository.getInstance().getSources() ;
        for( EventSource evtSrc : eventSources ) {
            log.debug( evtSrc.getClassName() + " :: " + evtSrc.getMethodName() ) ;
            evtSrc.getEvents().forEach( evt -> {
                log.debug( "\t" + evt.getEventName() ) ;
                evt.getEventTargets().forEach( tgt -> {
                    log.debug( "\t\t" + tgt.getClassName() + " :: " + tgt.getMethodName() ) ;
                } ) ;
            } ) ;
            log.debug( "" ) ;
        }
    }
}
