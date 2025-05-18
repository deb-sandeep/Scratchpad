package com.sandy.scratchpad.evtdoc.collector;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.sandy.scratchpad.evtdoc.Event;
import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import com.sandy.scratchpad.evtdoc.EventSource;

public class EventSrcCollector extends BaseEventCollector {
    
    @Override
    public void visit( MethodCallExpr n, Void arg ) {
        if( n.getNameAsExpression().toString().equals( "publishEvent" ) &&
            !curPkgName.contains( "com.sandy.sconsole.core.bus" ) ) {
            
            String evtName = n.getArguments().get( 0 ).toString() ;
            Event  event   = EvtRepository.getInstance().getEvent( evtName ) ;
            EventSource evtSrc = EvtPointRepository.getInstance().getSource( curPkgName, curClsName, curMthName ) ;
            
            evtSrc.getEvents().add( event ) ;
            event.getEventSources().add( evtSrc ) ;
        }
    }
}
