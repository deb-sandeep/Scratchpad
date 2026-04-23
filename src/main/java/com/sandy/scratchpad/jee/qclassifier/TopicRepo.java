package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;

@Slf4j
public class TopicRepo {

    private static TopicRepo instance ;

    public static TopicRepo instance() {
        if( instance == null ) {
            instance = new TopicRepo() ;
            instance.init() ;
        }
        return instance ;
    }

    private final Map<Integer, Topic> topicById = new HashMap<>();
    private final Map<String, List<Topic>> topicsBySyllabus = new HashMap<>();

    private TopicRepo() {}

    private void init() {
        try {
            InputStream is = getClass().getResourceAsStream( "/topic_master.json" ) ;
            if( is == null ) {
                log.error( "topic_master.json not found in classpath" ) ;
                return ;
            }
            ObjectMapper mapper = new ObjectMapper()
                    .setPropertyNamingStrategy( PropertyNamingStrategies.SNAKE_CASE ) ;
            
            List<Topic> topics = mapper.readValue( is, new TypeReference<>() {} ) ;
            for( Topic topic : topics ) {
                topicById.put( topic.getId(), topic ) ;
                topicsBySyllabus.computeIfAbsent( topic.getSyllabusName(), k -> new ArrayList<>() ).add( topic ) ;
            }
        }
        catch( Exception e ) {
            log.error( "Error loading topics", e ) ;
        }
    }
    
    public Topic getTopicById( int id ) {
        return topicById.get( id );
    }
    
    public List<Topic> getTopicsBySyllabus( String syllabusName ) {
        return topicsBySyllabus.getOrDefault( syllabusName, Collections.emptyList() );
    }
}
