package com.sandy.scratchpad.jee.qclassifier;

import lombok.Data;

@Data
public class Topic {
    private int id ;
    private String syllabusName ;
    private String section ;
    private String topicName ;
    private float matchProbability ;
}
