package com.sandy.scratchpad.jn.exercise;

import lombok.Data ;

@Data
public class ImgMeta implements Comparable<ImgMeta>{
    
    private int     chapterNum     = -1 ;
    private String  exerciseName   = null ;
    private int     questionNum    = 0 ;
    private int     subQuestionNum = -1 ;
    private boolean header         = false ;
    private boolean answer         = false ;
    private int     partNum        = -1 ;
    
    public ImgMeta( String fileName ) {
        parseFileName( fileName ) ;
    }
    
    private void parseFileName( String fileName ) {
        
        String fName = fileName ;
        
        // Strip the file extension
        if( fName.endsWith( ".png" ) ) {
            fName = fileName.substring( 0, fileName.length()-4 ) ;
        }
        
        // If this is a part, 
        if( fName.contains( "(" ) ) {
            int startIndex = fName.indexOf( "(" ) ;
            int endIndex   = fName.indexOf( ")", startIndex ) ;
            
            String partNumStr = fName.substring( startIndex+1, endIndex ) ;
            partNum = Integer.parseInt( partNumStr ) ;
            
            fName = fName.substring( 0, startIndex ) ;
        }
        
        String[] parts = fName.split( "_" ) ;
        
        // Extract the chapter number
        chapterNum = Integer.parseInt( parts[0].substring( 2 ) ) ;
        
        // Extract exercise name
        exerciseName = parts[1].trim() ;
        
        fName = parts[2] ;
        // If this is a header, extract the flag and strip 'Hdr'
        if( fName.endsWith( "Hdr" ) ) {
            header = true ;
            fName = fName.substring( 0, fName.length()-3 ) ;
        }
        else if( fName.endsWith( "Ans" ) ) {
            answer = true ;
            fName = fName.substring( 0, fName.length()-3 ) ;
        }
        
        // Parse question and sub-question number
        if( fName.contains( "." ) ) {
            parts = fName.split( "\\." ) ;
            questionNum = Integer.parseInt( parts[0] ) ;
            subQuestionNum = Integer.parseInt( parts[1] ) ;
        }
        else {
            questionNum = Integer.parseInt( fName ) ;
        }
    }
    
    public String getFileName() {
        
        StringBuilder buffer = new StringBuilder() ;
        
        buffer.append( "Ch" ).append( chapterNum ).append( "_" ) ;
        buffer.append( exerciseName ).append( "_" ) ;
        buffer.append( questionNum ) ;
        
        if( subQuestionNum != - 1 ) {
            buffer.append( "." ).append( subQuestionNum ) ;
        }
        
        if( header ) {
            buffer.append( "Hdr" ) ;
        }
        
        if( answer ) {
            buffer.append( "Ans" ) ;
        }
        
        if( partNum != -1 ) {
            buffer.append( "(" + partNum + ")" ) ;
        }
        
        buffer.append( ".png" ) ;
        
        return buffer.toString() ;
    }
    
    public String toString() {
        return getFileName() ;
    }
    
    @Override
    public int compareTo( ImgMeta m ) {
        if( chapterNum != m.chapterNum ) {
            return chapterNum - m.chapterNum ;
        }
        
        if( !exerciseName.equals( m.exerciseName ) ) {
            return exerciseName.compareTo( m.exerciseName ) ;
        }
        
        if( questionNum != m.questionNum ) {
            return questionNum - m.questionNum ;
        }
        
        if( header ) {
            return -1 ;
        }
        
        if( subQuestionNum != m.subQuestionNum ) {
            return subQuestionNum - m.subQuestionNum ;
        }
        
        if( partNum != m.partNum ) {
            return partNum - m.partNum ;
        }
        
        return 0 ;
    }

    public String getId() {
        StringBuilder sb = new StringBuilder() ;
        sb.append( getQuestionNum() ) ;
        if( getSubQuestionNum() > -1 ) {
            sb.append( "." ).append( getSubQuestionNum() ) ;
        }
        return sb.toString() ;
    }
    
    public String getGroupId() {
        if( subQuestionNum > -1 || header ) {
            return "" + questionNum ;
        }
        return null ;
    }
}


