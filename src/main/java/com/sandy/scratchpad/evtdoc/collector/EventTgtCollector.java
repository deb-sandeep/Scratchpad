package com.sandy.scratchpad.evtdoc.collector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.sandy.scratchpad.evtdoc.Event;
import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import com.sandy.scratchpad.evtdoc.EventTarget;
import org.apache.log4j.Logger;

import java.util.*;

class EventMeta {
    int priority ;
    boolean async ;
    
    EventMeta( int priority, boolean async ) {
        this.priority = priority ;
        this.async = async ;
    }
}

public class EventTgtCollector extends BaseEventCollector {
    
    private static final Logger log = Logger.getLogger( EventTgtCollector.class ) ;
    
    private final Map<String, EventTarget>   curEvtTgtMap  = new HashMap<>() ;
    private final HashMap<String, EventMeta> curEvtMetaMap = new HashMap<>() ;
    
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
        
        curEvtTgtMap.put( evtName, evtTgt ) ;
    }
    
    @Override
    public void visit( MethodCallExpr n, Void arg ) {
        
        String calledMethodName = n.getNameAsExpression().toString() ;
        if( calledMethodName.matches( "add.*Subscriber" ) &&
            !curPkgName.contains( "core.bus" ) ) {
            
            boolean isAsync = calledMethodName.contains( "Async" ) ;
            
            String evtName ;
            int priority = (isAsync ? 0 : 100) ; // This is the default priorities
            
            if( n.getArguments().size() > 2 ) {
                // Implies we have a subscriber with a specified priority
                String priorityStr = n.getArguments().get( 0 ).toString() ;
                evtName = n.getArguments().get( 2 ).toString() ;
                
                switch( priorityStr ) {
                    case "HIGH_PRIORITY", "EventBus.HIGH_PRIORITY" -> priority = 200 ;
                    case "LOW_PRIORITY", "EventBus.LOW_PRIORITY" -> priority = 50 ;
                    default -> priority = Integer.parseInt( priorityStr ) ;
                }
            }
            else {
                evtName = n.getArguments().get( 1 ).toString() ;
            }
            
            curEvtMetaMap.put( evtName, new EventMeta( priority, isAsync ) ) ;
        }
    }
    
    public void cuVisitComplete() {
        
        for( String evtName : curEvtMetaMap.keySet() ) {
            if( !curEvtTgtMap.containsKey( evtName ) ) {
                log.error( "Target for event " + evtName + " not found in class " + curClsName ) ;
            }
            else {
                EventTarget tgt = curEvtTgtMap.get( evtName ) ;
                EventMeta meta = curEvtMetaMap.get( evtName ) ;
                
                tgt.setSync( !meta.async ) ;
                tgt.setPriority( meta.priority ) ;
            }
        }
        curEvtMetaMap.clear() ;
        curEvtTgtMap.clear() ;
    }
}
