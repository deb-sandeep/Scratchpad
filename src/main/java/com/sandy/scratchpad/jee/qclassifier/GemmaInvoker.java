package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static com.sandy.scratchpad.jee.qclassifier.QImgClassifier.GEMMA_MODEL;
import static com.sandy.scratchpad.jee.qclassifier.QImgClassifier.GEMMA_SERVER_URL;

@Slf4j
public class GemmaInvoker {
    
    private final HttpClient httpClient = HttpClient.newHttpClient() ;
    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy( com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE ) ;
    
    private final String gemmaPrompt ;

    public GemmaInvoker() {
        String prompt = null ;
        try( InputStream is = getClass().getResourceAsStream( "/qclassifier.prompt" ) ) {
            assert is != null;
            prompt = new String( is.readAllBytes(), StandardCharsets.UTF_8 ) ;
        }
        catch( Exception e ) {
            log.error( "Error loading qclassifier.prompt", e ) ;
        }
        this.gemmaPrompt = prompt ;
    }

    private static final int MAX_ATTEMPTS = 2 ;

    public List<Topic> invoke( File imgFile ) {
        for( int attempt = 1 ; attempt <= MAX_ATTEMPTS ; attempt++ ) {
            List<Topic> result = attemptInvoke( imgFile, attempt ) ;
            if( result != null ) return result ;
            if( attempt < MAX_ATTEMPTS ) {
                log.warn( "Retrying Gemma invocation for {} (attempt {} of {})",
                        imgFile.getName(), attempt + 1, MAX_ATTEMPTS ) ;
            }
        }
        return null ;
    }

    private List<Topic> attemptInvoke( File imgFile, int attempt ) {
        long startTime = System.currentTimeMillis() ;
        try {
            ObjectNode payload = mapper.createObjectNode() ;
            payload.put( "model", GEMMA_MODEL ) ;
            payload.put( "prompt", createPrompt( imgFile ) ) ;
            payload.put( "stream", false ) ;
            payload.set( "images", mapper.valueToTree( getBase64EncodedImage( imgFile ) ) ) ;
            payload.put( "keep_alive", "5h" ) ;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( GEMMA_SERVER_URL ) )
                    .header( "Content-Type", "application/json" )
                    .POST( HttpRequest.BodyPublishers.ofString( mapper.writeValueAsString( payload ) ) )
                    .build() ;

            HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() ) ;
            if( response.statusCode() == 200 ) {
                String modelResponse = mapper.readTree( response.body() ).path( "response" ).asText() ;
                try {
                    if( modelResponse.startsWith( "{" ) && modelResponse.endsWith( "}" ) ) {
                        modelResponse = "[" + modelResponse + "]" ;
                    }
                    return mapper.readValue( modelResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Topic>>(){} ) ;
                }
                catch( JsonProcessingException e ) {
                    log.error( "Error parsing model response (attempt {}): {}", attempt, modelResponse, e ) ;
                    return null ;
                }
            }
            else {
                log.error( "Error invoking Gemma server (attempt {}, status {}): {}",
                        attempt, response.statusCode(), response.body() ) ;
            }
        }
        catch( Exception e ) {
            log.error( "Error invoking Gemma server (attempt {})", attempt, e ) ;
        }
        finally {
            long elapsed = System.currentTimeMillis() - startTime ;
            log.debug( "\t  Classification attempt {} took {}.{}s", attempt, elapsed / 1000, elapsed % 1000 ) ;
        }
        return null ;
    }
    
    private String[] getBase64EncodedImage( File... imgFiles ) {
        return java.util.Arrays.stream( imgFiles )
                .map( f -> {
                    try {
                        byte[] bytes = java.nio.file.Files.readAllBytes( f.toPath() ) ;
                        return java.util.Base64.getEncoder().encodeToString( bytes ) ;
                    }
                    catch( Exception e ) {
                        log.error( "Error encoding image {}", f.getAbsolutePath(), e ) ;
                        return null ;
                    }
                } )
                .toArray( String[]::new ) ;
    }
    
    private String createPrompt( File imgFile ) {
        return gemmaPrompt.replace( "{{TOPIC_LIST_JSON}}", getTopicList( imgFile ) ) ;
    }
    
    private String getTopicList( File imgFile ) {
        String questionId = FileHelper.extractQuestionId( imgFile ) ;
        TopicRepo topicRepo = TopicRepo.instance() ;
        List<Topic> topics = null ;
        if( questionId.startsWith( "C_" ) ) {
            topics = topicRepo.getTopicsBySyllabus( "IIT Chemistry" ) ;
        }
        else if( questionId.startsWith( "M_" ) ) {
            topics = topicRepo.getTopicsBySyllabus( "IIT Maths" ) ;
        }
        else if( questionId.startsWith( "P_" ) ) {
            topics = topicRepo.getTopicsBySyllabus( "IIT Physics" ) ;
        }
        if( topics == null ) return "" ;

        try {
            ArrayNode topicsArray = mapper.createArrayNode() ;
            for( Topic topic : topics ) {
                topicsArray.addObject()
                        .put( "topic_name", topic.getTopicName() )
                        .put( "id", topic.getId() ) ;
            }
            return mapper.writeValueAsString( topicsArray ) ;
        }
        catch( Exception e ) {
            log.error( "Error building topic list JSON", e ) ;
            return "" ;
        }
    }
}
