package com.sandy.scratchpad.evtdoc.collector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.sandy.scratchpad.evtdoc.Event;
import com.sandy.scratchpad.evtdoc.repo.EvtPointRepository;
import com.sandy.scratchpad.evtdoc.repo.EvtRepository;
import com.sandy.scratchpad.evtdoc.EventTarget;

import java.util.*;

class EventTgtMeta {
    int priority ;
    boolean async ;
    
    EventTgtMeta( int priority, boolean async ) {
        this.priority = priority ;
        this.async = async ;
    }
}

public class EventTgtCollector extends BaseEventCollector {
    
    private final Map<String, EventTarget> curEvtTgtMap  = new HashMap<>() ;
    private final HashMap<String, EventTgtMeta> curEvtMetaMap = new HashMap<>() ;
    
    // In this method we discover all add*Subscriber methods and keep track
    // of them along with their meta data (priority and event name). We do not
    // create event targets during this discovery. Event targets are created
    // when we find methods annotated with the EventTargetMarker annotation.
    // At the end, this bookkeeping data is reconciled to notify if all
    // subscribed events are translated into event targets.
    @Override
    public void visit( MethodCallExpr n, Void arg ) {
        
        String calledMethodName = n.getNameAsExpression().toString() ;
        
        if( calledMethodName.matches( "add.*Subscriber" ) &&
                !curPkgName.contains( "core.bus" ) ) {
            
            boolean isAsync = calledMethodName.contains( "Async" ) ;
            
            String evtName ;
            int priority = (isAsync ? 0 : 100) ; // These are the default priorities
            
            // We have two variants of add*Subscriber method.
            // 1 - Event Priority
            // 2 - Event subscriber reference
            // 3 - Event name.
            // Variety 1 has all three parameters while the second one as 2 and 3
            if( n.getArguments().size() > 2 ) { // Variety 1
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
            
            curEvtMetaMap.put( evtName, new EventTgtMeta( priority, isAsync ) ) ;
        }
    }
    
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
    
    public void cuVisitComplete() {
        
        for( String evtName : curEvtMetaMap.keySet() ) {
            if( !curEvtTgtMap.containsKey( evtName ) ) {
                throw new IllegalStateException( "Target for event " + evtName + " not found in class " + curClsName ) ;
            }
            else {
                EventTarget  tgt  = curEvtTgtMap.get( evtName ) ;
                EventTgtMeta meta = curEvtMetaMap.get( evtName ) ;
                
                tgt.setSync( !meta.async ) ;
                tgt.setPriority( meta.priority ) ;
            }
        }
        curEvtMetaMap.clear() ;
        curEvtTgtMap.clear() ;
    }
}
