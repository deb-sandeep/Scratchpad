package com.sandy.scratchpad.jn.quotes;

import static com.sandy.common.util.StringUtil.isEmptyOrNull ;

import lombok.Data ;

@Data
public class Quote {

    private String section = null ;
    private String quote = null ;
    private String speaker = null ;
    
    public Quote( String section, String quote, String speaker ) {
        this.section = isEmptyOrNull( section ) ? null : section.trim() ;
        this.quote   = isEmptyOrNull( quote )   ? null : quote.trim() ;
        this.speaker = isEmptyOrNull( speaker ) ? null : speaker.trim() ;
    }
    
    public String toString() {
        return "[" + section + "] " + quote + " -" + speaker ;
    }
    
    public String getInsertQuery() {
        return "insert into jove_notes.quote_master " + 
               "( section, speaker, quote ) " + 
               "values ( " + 
               "\"" + this.section + "\"," + 
               "\"" + this.speaker + "\"," + 
               "\"" + this.quote + "\") ;" ;
    }
}
