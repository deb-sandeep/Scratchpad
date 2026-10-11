package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.sandy.scratchpad.jee.qclassifier.QImgClassifier.GEMMA_MODEL;
import static com.sandy.scratchpad.jee.qclassifier.QImgClassifier.GEMMA_SERVER_URL;

@Slf4j
public class GemmaInvoker {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds( 10 ) ;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes( 5 ) ;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout( CONNECT_TIMEOUT )
            .build() ;
    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy( com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE ) ;

    /** Shared classifier prompt used for Physics and Maths. */
    private final String defaultPrompt ;
    /** Chemistry-specific prompt (branch-first + disambiguation guidance). */
    private final String chemistryPrompt ;
    /** Per-subject syllabus scope reference, keyed by "C" / "M" / "P". */
    private final Map<String,String> syllabusBySubject = new HashMap<>() ;

    public GemmaInvoker() {
        this.defaultPrompt   = loadResource( "/qclassifier.prompt" ) ;
        this.chemistryPrompt = loadResource( "/qclassifier.chemistry.prompt" ) ;
        syllabusBySubject.put( "C", loadResource( "/qclassifier.chemistry.syllabus.txt" ) ) ;
        syllabusBySubject.put( "P", loadResource( "/qclassifier.physics.syllabus.txt" ) ) ;
        syllabusBySubject.put( "M", loadResource( "/qclassifier.maths.syllabus.txt" ) ) ;
    }

    private String loadResource( String path ) {
        try( InputStream is = getClass().getResourceAsStream( path ) ) {
            assert is != null ;
            return new String( is.readAllBytes(), StandardCharsets.UTF_8 ) ;
        }
        catch( Exception e ) {
            log.error( "Error loading resource {}", path, e ) ;
            return "" ;
        }
    }

    private static final int MAX_ATTEMPTS = 2 ;

    /**
     * Context window requested from Ollama. The classifier prompt (instructions +
     * syllabus reference + topic list) plus the encoded image runs to several
     * thousand tokens; Ollama's default num_ctx (4096) would silently truncate it.
     * The Chemistry prompt (largest) is ~5.5k text tokens plus the encoded image.
     */
    private static final int NUM_CTX = 12288 ;

    /** Outcome of an {@link #invoke} call. {@code topics} is null on failure. */
    public static final class InvocationResult {
        public final List<Topic> topics ;
        public final int         attempts ;
        public final String      failureReason ;   // null on success

        InvocationResult( List<Topic> topics, int attempts, String failureReason ) {
            this.topics        = topics ;
            this.attempts      = attempts ;
            this.failureReason = failureReason ;
        }
    }

    public InvocationResult invoke( QTask task ) {
        return invoke( task, null ) ;
    }

    public InvocationResult invoke( QTask task, BooleanSupplier keepGoing ) {
        String prompt = createPrompt( task.getSubject() ) ;
        String image ;
        try {
            // Multi-part questions are stacked into one image so the model sees the whole question.
            image = Base64.getEncoder().encodeToString( ImageCollator.collatedBytes( task.getImgFiles() ) ) ;
        }
        catch( Exception e ) {
            log.error( "Error preparing image for {}", task, e ) ;
            return new InvocationResult( null, 0, "image error: " + e.getMessage() ) ;
        }

        String lastFailure = "unknown error" ;
        for( int attempt = 1 ; attempt <= MAX_ATTEMPTS ; attempt++ ) {
            InvocationResult outcome = attemptInvoke( task, prompt, image, attempt ) ;
            if( outcome.topics != null ) return outcome ;
            lastFailure = outcome.failureReason ;
            if( attempt < MAX_ATTEMPTS ) {
                if( keepGoing != null && !keepGoing.getAsBoolean() ) {
                    log.debug( "Stop requested - not retrying {}", task ) ;
                    break ;
                }
                log.warn( "Retrying Gemma invocation for {} (attempt {} of {})",
                        task, attempt + 1, MAX_ATTEMPTS ) ;
            }
        }
        return new InvocationResult( null, MAX_ATTEMPTS, lastFailure ) ;
    }

    private InvocationResult attemptInvoke( QTask task, String prompt, String image, int attempt ) {
        long startTime = System.currentTimeMillis() ;
        String failureReason = "unknown error" ;
        try {
            ObjectNode payload = mapper.createObjectNode() ;
            payload.put( "model", GEMMA_MODEL ) ;
            log.debug( "\t  Prompt for {} is {} chars (num_ctx {})",
                    task, prompt.length(), NUM_CTX ) ;
            payload.put( "prompt", prompt ) ;
            payload.put( "stream", false ) ;
            payload.set( "images", mapper.createArrayNode().add( image ) ) ;
            payload.put( "keep_alive", "5h" ) ;
            payload.set( "options", mapper.createObjectNode().put( "num_ctx", NUM_CTX ) ) ;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( GEMMA_SERVER_URL ) )
                    .header( "Content-Type", "application/json" )
                    .timeout( REQUEST_TIMEOUT )
                    .POST( HttpRequest.BodyPublishers.ofString( mapper.writeValueAsString( payload ) ) )
                    .build() ;

            HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() ) ;
            if( response.statusCode() == 200 ) {
                com.fasterxml.jackson.databind.JsonNode body = mapper.readTree( response.body() ) ;
                int promptTokens = body.path( "prompt_eval_count" ).asInt() ;
                if( promptTokens >= NUM_CTX ) {
                    log.warn( "Prompt for {} used {} tokens - at or beyond num_ctx {}; input may be truncated",
                            task, promptTokens, NUM_CTX ) ;
                }
                String modelResponse = body.path( "response" ).asText() ;
                try {
                    if( modelResponse.startsWith( "{" ) && modelResponse.endsWith( "}" ) ) {
                        modelResponse = "[" + modelResponse + "]" ;
                    }
                    List<Topic> topics = mapper.readValue( modelResponse,
                            new com.fasterxml.jackson.core.type.TypeReference<List<Topic>>(){} ) ;
                    return new InvocationResult( topics, attempt, null ) ;
                }
                catch( JsonProcessingException e ) {
                    log.error( "Error parsing model response (attempt {}): {}", attempt, modelResponse, e ) ;
                    failureReason = "unparseable model response" ;
                }
            }
            else {
                log.error( "Error invoking Gemma server (attempt {}, status {}): {}",
                        attempt, response.statusCode(), response.body() ) ;
                failureReason = "HTTP " + response.statusCode() ;
            }
        }
        catch( Exception e ) {
            log.error( "Error invoking Gemma server (attempt {})", attempt, e ) ;
            failureReason = e.getClass().getSimpleName() +
                    ( e.getMessage() != null ? ": " + e.getMessage() : "" ) ;
        }
        finally {
            long elapsed = System.currentTimeMillis() - startTime ;
            log.debug( "\t  Classification attempt {} took {}.{}s", attempt, elapsed / 1000, elapsed % 1000 ) ;
        }
        return new InvocationResult( null, attempt, failureReason ) ;
    }
    
    /** Prompt for subject code "C" / "M" / "P" (anything else gets the default prompt with no topics). */
    private String createPrompt( String subject ) {
        String template = "C".equals( subject ) ? chemistryPrompt : defaultPrompt ;
        String syllabus = syllabusBySubject.getOrDefault( subject, "" ) ;
        return template
                .replace( "{{SUBJECT_SYLLABUS}}", syllabus )
                .replace( "{{TOPIC_LIST_JSON}}", getTopicList( subject ) ) ;
    }

    private String getTopicList( String subject ) {
        String syllabusName = TopicRepo.syllabusForSubject( subject ) ;
        if( syllabusName == null ) return "" ;

        boolean isChemistry = "C".equals( subject ) ;
        List<Topic> topics = TopicRepo.instance().getTopicsBySyllabus( syllabusName ) ;
        try {
            // Flat list. The syllabus reference already groups topics by branch;
            // keeping the id-copy task flat matches the format that works
            // reliably for Physics/Maths. Chemistry additionally carries the
            // branch name so the model can reason branch-first.
            ArrayNode topicsArray = mapper.createArrayNode() ;
            for( Topic topic : topics ) {
                ObjectNode node = topicsArray.addObject()
                        .put( "topic_name", topic.getTopicName() ) ;
                if( isChemistry ) node.put( "branch", topic.getSection() ) ;
                node.put( "id", topic.getId() ) ;
            }
            return mapper.writeValueAsString( topicsArray ) ;
        }
        catch( Exception e ) {
            log.error( "Error building topic list JSON", e ) ;
            return "" ;
        }
    }
}
