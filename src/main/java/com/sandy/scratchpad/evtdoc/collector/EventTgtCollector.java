package com.sandy.scratchpad.evtdoc.collector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.sandy.scratchpad.evtdoc.Event;
import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import com.sandy.scratchpad.evtdoc.EventTarget;
import org.apache.log4j.Logger;

import java.util.*;

public class EventTgtCollector extends BaseEventCollector {
    
    private static final Logger log = Logger.getLogger( EventTgtCollector.class ) ;
    
    private Map<String, EventTarget> evtMap = new HashMap<>() ;
    private Set<String> evtSet = new HashSet<>() ;
    
    @Override
    public void visit( MethodDeclaration n, Void arg ) {
        this.curMthName = n.getNameAsString() ;
        
        n.getAnnotations().forEach( a -> {
            if( a.getNameAsString().equals( "EventTargetMarker" ) ) {
                SingleMemberAnnotationExpr smae = (SingleMemberAnnotationExpr)a ;
                Expression memberValue = smae.getMemberValue() ;
        
                if( memberValue instanceof NameExpr ) {
                    String evtName = ( ( NameExpr )memberValue ).getNameAsString() ;
                    addEventTarget( evtName ) ;
                }
                else if( memberValue instanceof ArrayInitializerExpr ) {
                    ArrayInitializerExpr aie = (ArrayInitializerExpr)memberValue ;
                    aie.getValues().forEach( v -> {
                        String evtName = ( ( NameExpr )v ).getNameAsString() ;
                        addEventTarget( evtName );
                    }) ;
                }
            }
        } ) ;
        super.visit( n, arg ) ;
    }
    
    private void addEventTarget( String evtName ) {
        
        Event event = EvtRepository.getInstance().getEvent( evtName ) ;
        EventTarget evtTgt = EvtPointRepository.getInstance().getTarget( curPkgName, curClsName, curMthName ) ;
        
        evtTgt.getEvents().add( event ) ;
        event.getEventTargets().add( evtTgt ) ;
        
        evtMap.put( evtName, evtTgt ) ;
    }
    
    @Override
    public void visit( MethodCallExpr n, Void arg ) {
        String calledMethodName = n.getNameAsExpression().toString() ;
        
        if( calledMethodName.matches( "add.*Subscriber" ) &&
            !curPkgName.contains( "core.bus" ) ) {
            
            String evtName = n.getArguments().get( 1 ).toString() ;
            evtSet.add( evtName ) ;
        }
    }
    
    public void cuVisitComplete() {
        for( String evtName : evtSet ) {
            if( !evtMap.containsKey( evtName ) ) {
                log.error( "Target for event " + evtName + " not found in class " + curClsName ) ;
            }
        }
    }
}
