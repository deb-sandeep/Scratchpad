package com.sandy.scratchpad.jee.qclassifier;

/**
 * One AI backend that can classify a question image. {@link AIInvoker}
 * builds the prompt, spreads tasks across processors and parses the reply;
 * a processor only makes a single request and returns the model's raw text.
 */
public interface AIProcessor {

    /** Short label used in logs, e.g. "gemma" or "openrouter". */
    String getName() ;

    /** How many requests this processor may have in flight at once. */
    int getMaxParallel() ;

    /**
     * Send one classification request.
     *
     * @param imageBase64 the question image, base64 encoded
     * @param mimeType    the image's media type, e.g. "image/png"
     * @return the model's text reply (expected to be the JSON topic array)
     * @throws ProcessorException with a short reason when the request fails
     */
    String complete( String prompt, String imageBase64, String mimeType ) throws ProcessorException ;

    class ProcessorException extends Exception {
        public ProcessorException( String reason ) {
            super( reason ) ;
        }
    }
}
