package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Classifies a question by sending it to one of several {@link AIProcessor}s.
 * Each processor has its own number of parallel slots; a request goes to the
 * least-loaded processor with a free slot, so adding processors adds
 * throughput. A failed attempt is retried, preferring a different processor.
 */
@Slf4j
public class AIInvoker {

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy( com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE ) ;

    /** Shared classifier prompt used for Physics and Maths. */
    private final String defaultPrompt ;
    /** Chemistry-specific prompt (branch-first + disambiguation guidance). */
    private final String chemistryPrompt ;
    /** Per-subject syllabus scope reference, keyed by "C" / "M" / "P". */
    private final Map<String,String> syllabusBySubject = new HashMap<>() ;

    private final List<AIProcessor> processors ;
    /** Requests in flight per processor (same index as {@link #processors}); guarded by {@link #slotLock}. */
    private final int[]  busy ;
    private final Object slotLock = new Object() ;

    public AIInvoker( List<AIProcessor> processors ) {
        if( processors.isEmpty() ) throw new IllegalArgumentException( "At least one AI processor is required" ) ;
        this.processors = List.copyOf( processors ) ;
        this.busy       = new int[ processors.size() ] ;

        this.defaultPrompt   = loadResource( "/qclassifier.prompt" ) ;
        this.chemistryPrompt = loadResource( "/qclassifier.chemistry.prompt" ) ;
        syllabusBySubject.put( "C", loadResource( "/qclassifier.chemistry.syllabus.txt" ) ) ;
        syllabusBySubject.put( "P", loadResource( "/qclassifier.physics.syllabus.txt" ) ) ;
        syllabusBySubject.put( "M", loadResource( "/qclassifier.maths.syllabus.txt" ) ) ;
    }

    /** Builds the processors enabled in {@code config}. */
    public static AIInvoker fromConfig( QClassifierConfig config ) {
        List<AIProcessor> ps = new ArrayList<>() ;
        if( config.gemmaEnabled ) {
            ps.add( new OllamaProcessor( config.gemmaServerUrl, config.gemmaModel, config.gemmaMaxParallel ) ) ;
            log.info( "AI processor: gemma ({} @ {}), {} parallel",
                    config.gemmaModel, config.gemmaServerUrl, config.gemmaMaxParallel ) ;
        }
        if( config.openRouterEnabled ) {
            if( config.openRouterApiKey == null ) {
                log.error( "OpenRouter is enabled but no API key was found - skipping it" ) ;
            }
            else {
                ps.add( new OpenRouterProcessor( config.openRouterApiKey, config.openRouterModel,
                        config.openRouterMaxParallel ) ) ;
                log.info( "AI processor: openrouter ({}), {} parallel",
                        config.openRouterModel, config.openRouterMaxParallel ) ;
            }
        }
        return new AIInvoker( ps ) ;
    }

    /** Total parallel slots across all processors. */
    public int getMaxParallel() {
        return processors.stream().mapToInt( AIProcessor::getMaxParallel ).sum() ;
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
        String mimeType = ImageCollator.collatedMimeType( task.getImgFiles() ) ;

        String lastFailure = "unknown error" ;
        AIProcessor lastFailed = null ;
        for( int attempt = 1 ; attempt <= MAX_ATTEMPTS ; attempt++ ) {
            AIProcessor processor ;
            try {
                processor = acquire( lastFailed ) ;
            }
            catch( InterruptedException e ) {
                Thread.currentThread().interrupt() ;
                return new InvocationResult( null, attempt - 1, "interrupted" ) ;
            }

            InvocationResult outcome ;
            try {
                outcome = attemptInvoke( processor, task, prompt, image, mimeType, attempt ) ;
            }
            finally {
                release( processor ) ;
            }
            if( outcome.topics != null ) return outcome ;
            lastFailure = outcome.failureReason ;
            lastFailed  = processor ;
            if( attempt < MAX_ATTEMPTS ) {
                if( Thread.currentThread().isInterrupted() ||
                    ( keepGoing != null && !keepGoing.getAsBoolean() ) ) {
                    log.debug( "Stop requested - not retrying {}", task ) ;
                    break ;
                }
                log.warn( "Retrying invocation for {} (attempt {} of {})",
                        task, attempt + 1, MAX_ATTEMPTS ) ;
            }
        }
        return new InvocationResult( null, MAX_ATTEMPTS, lastFailure ) ;
    }

    /**
     * Claim a slot on the least-loaded processor that has one free, waiting if
     * all are busy. {@code avoid} (the processor that just failed) is only
     * chosen when no other processor has a free slot.
     */
    private AIProcessor acquire( AIProcessor avoid ) throws InterruptedException {
        synchronized( slotLock ) {
            while( true ) {
                int best = -1 ;
                double bestLoad = Double.MAX_VALUE ;
                for( int i = 0 ; i < processors.size() ; i++ ) {
                    AIProcessor p = processors.get( i ) ;
                    if( busy[i] >= p.getMaxParallel() ) continue ;
                    double load = (double) busy[i] / p.getMaxParallel() + ( p == avoid ? 1 : 0 ) ;
                    if( load < bestLoad ) {
                        best = i ;
                        bestLoad = load ;
                    }
                }
                if( best >= 0 ) {
                    busy[best]++ ;
                    return processors.get( best ) ;
                }
                slotLock.wait() ;
            }
        }
    }

    private void release( AIProcessor processor ) {
        synchronized( slotLock ) {
            busy[ processors.indexOf( processor ) ]-- ;
            slotLock.notifyAll() ;
        }
    }

    private InvocationResult attemptInvoke( AIProcessor processor, QTask task, String prompt,
                                            String image, String mimeType, int attempt ) {
        long startTime = System.currentTimeMillis() ;
        String failureReason ;
        try {
            log.debug( "\t  Sending {} to {} (attempt {}, prompt {} chars)",
                    task, processor.getName(), attempt, prompt.length() ) ;
            String modelResponse = cleanResponse( processor.complete( prompt, image, mimeType ) ) ;
            try {
                List<Topic> topics = mapper.readValue( modelResponse,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Topic>>(){} ) ;
                return new InvocationResult( topics, attempt, null ) ;
            }
            catch( JsonProcessingException e ) {
                log.error( "Error parsing {} response (attempt {}): {}",
                        processor.getName(), attempt, modelResponse, e ) ;
                failureReason = "unparseable " + processor.getName() + " response" ;
            }
        }
        catch( AIProcessor.ProcessorException e ) {
            failureReason = e.getMessage() ;
        }
        finally {
            long elapsed = System.currentTimeMillis() - startTime ;
            log.debug( "\t  {} attempt {} on {} took {}.{}s",
                    task, attempt, processor.getName(), elapsed / 1000, elapsed % 1000 ) ;
        }
        return new InvocationResult( null, attempt, failureReason ) ;
    }

    /** Strip markdown fences some models add despite the prompt, and wrap a lone object in an array. */
    private static String cleanResponse( String response ) {
        String s = response.trim() ;
        if( s.startsWith( "```" ) ) {
            int firstNewline = s.indexOf( '\n' ) ;
            s = firstNewline < 0 ? "" : s.substring( firstNewline + 1 ) ;
            if( s.endsWith( "```" ) ) s = s.substring( 0, s.length() - 3 ) ;
            s = s.trim() ;
        }
        if( s.startsWith( "{" ) && s.endsWith( "}" ) ) s = "[" + s + "]" ;
        return s ;
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
