package com.sandy.scratchpad.jn.marathi;

public class MarathiFragment {

    private String marathiText = "" ;
    private String imgResourcePath = null ;
    private String audioResourcePath = null ;
    private String englishText = "" ;
    private String pronunciation = "" ;
    
    public String getMarathiText() {
        return marathiText ;
    }
    public void setMarathiText( String marathiText ) {
        this.marathiText = marathiText ;
    }
    
    public String getImgResourcePath() {
        return imgResourcePath ;
    }
    public void setImgResourcePath( String imgResourcePath ) {
        this.imgResourcePath = imgResourcePath ;
    }
    
    public String getAudioResourcePath() {
        return audioResourcePath ;
    }
    public void setAudioResourcePath( String audioResourcePath ) {
        this.audioResourcePath = audioResourcePath ;
    }
    
    public String getEnglishText() {
        return englishText ;
    }
    public void setEnglishText( String englishText ) {
        this.englishText = englishText ;
    }
    
    public String getPronunciation() {
        return pronunciation ;
    }
    public void setPronunciation( String pronunciation ) {
        this.pronunciation = pronunciation ;
    }
    
    public String toString() {
        return      "  marathiText = " + marathiText
                + "\n\t imgResourcePath = " + imgResourcePath
                + "\n\t audioResourcePath = " + audioResourcePath 
                + "\n\t englishText = " + englishText 
                + "\n\t pronunciation = " + pronunciation ;
    }
}
