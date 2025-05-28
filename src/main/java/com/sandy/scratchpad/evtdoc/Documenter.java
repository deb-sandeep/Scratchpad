package com.sandy.scratchpad.evtdoc;

import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class Documenter {
    
    private static final Logger log = Logger.getLogger( Documenter.class ) ;
    private static final File EVENT_FLOW_SRC_ROOT = new File( "/Users/sandeep/projects/sconsole/SConsoleNxt/doc/event-flow-src-root.txt" ) ;
    private static final File EVENT_FLOW_EVT_ROOT = new File( "/Users/sandeep/projects/sconsole/SConsoleNxt/doc/event-flow-evt-root.txt" ) ;
    
    private Collection<EventSource> eventSources = null ;

    public Documenter() {}
    
    public void generateDocumentation() throws IOException {
        
        log.debug( "Generating documentation ..." ) ;
        this.eventSources = EvtPointRepository.getInstance().getSources() ;
        StringBuilder sb = new StringBuilder() ;
        for( EventSource evtSrc : this.eventSources ) {
            documentSrcRoot( evtSrc, "", false, 0, true, sb ) ;
            sb.append( "\n" ) ;
        }
        log.debug( sb.toString() ) ;
        FileUtils.write( EVENT_FLOW_SRC_ROOT, sb.toString() ) ;
        
        log.debug( "\n===================================================\n" ) ;
        sb = new StringBuilder() ;
        for( Event evt : EvtRepository.getInstance().getEvents() ) {
            documentEvtRoot( evt, sb ) ;
            sb.append( "\n" ) ;
        }
        log.debug( sb.toString() ) ;
        FileUtils.write( EVENT_FLOW_EVT_ROOT, sb.toString() ) ;
    }
    
    private void documentSrcRoot( EventSource evtSrc, String indent, boolean isSync, int priority, boolean isRoot, StringBuilder sb ) {
        
        sb.append( indent + srcTgtPreamble( isSync, isRoot, priority ) + evtSrc.getClassName() + " : " + evtSrc.getMethodName() + "\n" ) ;
        
        evtSrc.getEvents().forEach( evt -> {
            
            sb.append( indent + "\t" + (isRoot ? "" : "  " ) + evt.getEventName() + "\n"  ) ;
            
            if( evt.getPrioritizedEventTargets().isEmpty() ) {
                sb.append( indent + "\t\t" + "[NO EVENT TARGETS FOUND]" + "\n"  ) ;
            }
            else {
                evt.getPrioritizedEventTargets().forEach( tgt -> {
                    
                    EventSource nextSrc = getEventSource( tgt.getPackageName(), tgt.getClassName(), tgt.getMethodName() ) ;
                    if( nextSrc != null ) {
                        documentSrcRoot( nextSrc, indent + "\t\t", tgt.isSync(), tgt.getPriority(), false, sb ) ;
                    }
                    else {
                        sb.append( indent + "\t\t" + srcTgtPreamble( tgt.isSync(), false, tgt.getPriority() ) + tgt.getClassName() + " : " + tgt.getMethodName() + "\n" ) ;
                    }
                } ) ;
            }
        } ) ;
    }
    
    private void documentEvtRoot( Event evt, StringBuilder sb ) {
        
        sb.append( evt.getEventName() + "\n"  ) ;
        sb.append( "\t" + "Event Sources:" + "\n"  ) ;
        if( evt.getEventSources().isEmpty() ) {
            sb.append( "\t\t[NO EVENT SOURCES FOUND]" + "\n"  ) ;
        }
        else {
            evt.getEventSources().forEach( src ->
                    sb.append( "\t\t" + src.getClassName() + " : " + src.getMethodName()  + "\n" ) ) ;
        }
        
        sb.append( "\t" + "Event Targets:" + "\n"  ) ;
        if( evt.getEventTargets().isEmpty() ) {
            sb.append( "\t\t[NO EVENT TARGETS FOUND]" + "\n"  ) ;
        }
        else {
            evt.getEventTargets().forEach( tgt -> {
                EventSource nextSrc = getEventSource( tgt.getPackageName(), tgt.getClassName(), tgt.getMethodName() ) ;
                if( nextSrc != null ) {
                    documentSrcRoot( nextSrc, "\t\t", tgt.isSync(), tgt.getPriority(), false, sb ) ;
                }
                else {
                    sb.append( "\t\t" + srcTgtPreamble( tgt.isSync(), false, tgt.getPriority() ) + tgt.getClassName() + " : " + tgt.getMethodName() + "\n"  ) ;
                }
            } ) ;
        }
    }
    
    private String srcTgtPreamble( boolean isSync, boolean isRoot, int priority ) {
        if( !isRoot ) {
            return ( isSync ? "(*) " : "(~) " ) +
                   ( priority > 0 ? "[" + priority + "] " : "" ) ;
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
