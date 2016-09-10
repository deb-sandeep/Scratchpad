package com.sandy.scratchpad.jn.exercise;


public class Question implements Comparable<Question> {
    
    private boolean isExample     = false ;
    private String  exerciseName  = null ;
    private boolean isAnswer      = false ;
    private int     partNumber    = -1 ;
    private int[]   sequenceParts = null ;

    public void setExample( boolean isExample ) {
        this.isExample = isExample ;
    }
    
    public boolean isExample() {
        return isExample ;
    }
    
    public boolean isExercise() {
        return !isExample() ;
    }
    
    public void setExerciseName( String exName ) {
        this.exerciseName = exName ;
    }
    
    public String getExerciseName() {
        return this.exerciseName ;
    }
    
    public void setAnswer( boolean isAnswer ) {
        this.isAnswer = isAnswer ;
    }
    
    public boolean isAnswer() {
        return this.isAnswer ;
    }
    
    public void setPartNumber( int partNum ) {
        this.partNumber = partNum ;
    }
    
    public boolean isPart() {
        return this.partNumber != -1 ;
    }
    
    public void setSequenceParts( int[] seqParts ) {
        this.sequenceParts = seqParts ;
    }
    
    public int[] getSequenceParts() {
        return this.sequenceParts ;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder() ;
        
        if( isExample() ) {
            buffer.append( "ex_" ) ;
        }
        else {
            buffer.append( "Ex" ).append( getExerciseName() ).append( "_" ) ;
        }
        
        for( int i=0; i<sequenceParts.length; i++ ) {
            buffer.append( sequenceParts[i] ) ;
            if( i < sequenceParts.length-1 ) {
                buffer.append( "." ) ;
            }
        }
        
        if( isPart() ) {
            buffer.append( "(" ).append( this.partNumber ).append( ")" ) ;
        }
        
        if( isAnswer() ) {
            buffer.append( "Ans" ) ;
        }
        
        return buffer.toString() ;
    }

    @Override
    public int compareTo( Question q ) {
        
        if( this.isExample() && !q.isExample() ) {
            return Integer.MIN_VALUE ;
        }
        else if( !this.isExample() && q.isExample() ) {
            return Integer.MAX_VALUE ;
        }
        else {
            
            if( !this.isExample() && !q.isExample() ) {
                int comp = getExerciseName().compareTo( q.getExerciseName() ) ;
                if( comp != 0 ) {
                    return comp ;
                }
            }
            
            int seqCompare = compare( this.sequenceParts, q.sequenceParts ) ;
            if( seqCompare == 0 ) {
                if( this.isAnswer && !q.isAnswer ) {
                    return Integer.MAX_VALUE ;
                }
                else if( !this.isAnswer && q.isAnswer ) {
                    return Integer.MIN_VALUE ;
                }
                else {
                    if( this.isPart() && q.isPart() ) {
                        return Integer.compare( this.partNumber, q.partNumber ) ;
                    }
                    else if( !this.isPart() && q.isPart() ) {
                        return Integer.MIN_VALUE ;
                    }
                    else if( this.isPart() && !q.isPart() ) {
                        return Integer.MAX_VALUE ;
                    }
                }
            }
            else {
                return seqCompare ;
            }
        }
        
        return 0 ;
    }
    
    private int compare( int[] seqA, int[] seqB ) {
        int loopSize = Math.min( seqA.length, seqB.length ) ;
        for( int i=0; i<loopSize; i++ ) {
            if( seqA[i] != seqB[i] ) {
                return Integer.compare( seqA[i], seqB[i] ) ; 
            }
        }
        return Integer.compare( seqA.length, seqB.length ) ;
    }
}
