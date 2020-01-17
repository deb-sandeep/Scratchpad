package com.sandy.scratchpad.aws;

public class AWSProductMeta {

    private String productCategory = null ;
    private String productName = null ;
    private String shortDescription = null ;
    private String longDescription = null ;
    private String url = null ;
    
    public String getProductCategory() {
        return productCategory ;
    }
    public void setProductCategory( String productCategory ) {
        this.productCategory = productCategory ;
    }
    
    public String getProductName() {
        return productName ;
    }
    public void setProductName( String productName ) {
        this.productName = productName ;
    }
    
    public String getShortDescription() {
        return shortDescription ;
    }
    public void setShortDescription( String shortDescription ) {
        this.shortDescription = shortDescription ;
    }
    
    public String getUrl() {
        return url ;
    }
    public void setUrl( String url ) {
        this.url = url ;
    }
    
    public String toString() {
        return "AWSProductMeta [productCategory=" + productCategory
                + ", productName=" + productName + ", shortDescription="
                + shortDescription + ", url=" + url + "]" ;
    }
    
    public void setLongDescription( String string ) {
        this.longDescription = string ;
    }
    
    public String getLongDescription() {
        return this.longDescription ;
    }
}
