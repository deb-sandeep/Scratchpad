package com.sandy.scratchpad.evtdoc;

import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import org.apache.log4j.Logger;

import java.util.Collection;

public class Documenter {
    
    private static final Logger log = Logger.getLogger( Documenter.class ) ;
    
    private Collection<EventSource> eventSources = null ;

    public Documenter() {}
    
    public void generateDocumentation() {
        log.debug( "Generating documentation ..." ) ;
        this.eventSources = EvtPointRepository.getInstance().getSources() ;
        for( EventSource evtSrc : this.eventSources ) {
            documentSrcRoot( evtSrc, "", false, 0, true ) ;
            log.debug( "" ) ;
        }
        
        log.debug( "\n===================================================\n" ) ;
        for( Event evt : EvtRepository.getInstance().getEvents() ) {
            documentEvtRoot( evt ) ;
            log.debug( "" ) ;
        }
    }
    
    private void documentSrcRoot( EventSource evtSrc, String indent, boolean isSync, int priority, boolean isRoot ) {
        
        log.debug( indent + srcTgtPreamble( isSync, isRoot, priority ) + evtSrc.getClassName() + " :: " + evtSrc.getMethodName() ) ;
        
        evtSrc.getEvents().forEach( evt -> {
            
            log.debug( indent + "\t" + (isRoot ? "" : "  " ) + evt.getEventName() ) ;
            
            evt.getPrioritizedEventTargets().forEach( tgt -> {
                
                EventSource nextSrc = getEventSource( tgt.getPackageName(), tgt.getClassName(), tgt.getMethodName() ) ;
                if( nextSrc != null ) {
                    documentSrcRoot( nextSrc, indent + "\t\t", tgt.isSync(), tgt.getPriority(), false ) ;
                }
                else {
                    log.debug( indent + "\t\t" + srcTgtPreamble( tgt.isSync(), false, tgt.getPriority() ) + tgt.getClassName() + " :: " + tgt.getMethodName() ) ;
                }
            } ) ;
        } ) ;
    }
    
    private void documentEvtRoot( Event evt ) {
        
        log.debug( evt.getEventName() ) ;
        log.debug( "\t" + "Event Sources:" ) ;
        evt.getEventSources().forEach( src ->
                log.debug( "\t\t" + src.getClassName() + " :: " + src.getMethodName() ) ) ;
        
        log.debug( "\t" + "Event Targets:" ) ;
        evt.getEventTargets().forEach( tgt -> {
            EventSource nextSrc = getEventSource( tgt.getPackageName(), tgt.getClassName(), tgt.getMethodName() ) ;
            if( nextSrc != null ) {
                documentSrcRoot( nextSrc, "\t\t", tgt.isSync(), tgt.getPriority(), false ) ;
            }
            else {
                log.debug( "\t\t" + srcTgtPreamble( tgt.isSync(), false, tgt.getPriority() ) + tgt.getClassName() + " :: " + tgt.getMethodName() ) ;
            }
        } ) ;
    }
    
    private String srcTgtPreamble( boolean isSync, boolean isRoot, int priority ) {
        if( !isRoot ) {
            return (isSync ? "(*) " : "(~) " ) + ( priority > 0 ? "[" + priority + "] " : "" ) ;
        }
        return "" ;
    }
    
    private EventSource getEventSource( String pkgName, String clsName, String mthName ) {
        for( EventSource evtSrc : this.eventSources ) {
            if( evtSrc.getPackageName().equals( pkgName ) &&
                evtSrc.getClassName().equals( clsName ) &&
                evtSrc.getMethodName().equals( mthName ) ) {
                
                return evtSrc ;
            }
        }
        return null ;
    }
}
