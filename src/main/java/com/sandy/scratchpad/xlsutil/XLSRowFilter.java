package com.sandy.scratchpad.xlsutil;

public interface XLSRowFilter {
    
    abstract static class BinaryLogicFilter implements XLSRowFilter {
        
        protected XLSRowFilter f1 = null ;
        protected XLSRowFilter f2 = null ;
        
        public BinaryLogicFilter( XLSRowFilter f1, XLSRowFilter f2 ) {
            this.f1 = f1 ;
            this.f2 = f2 ;
        }
    }
    
    public static class AND extends BinaryLogicFilter {
        
        public AND( XLSRowFilter f1, XLSRowFilter f2 ) {
            super( f1, f2 ) ;
        }

        public boolean accept( XLSRow row ) {
            return f1.accept( row ) && f2.accept( row ) ;
        }
    }

    public static class OR extends BinaryLogicFilter {
        
        public OR( XLSRowFilter f1, XLSRowFilter f2 ) {
            super( f1, f2 ) ;
        }

        public boolean accept( XLSRow row ) {
            return f1.accept( row ) || f2.accept( row ) ;
        }
    }
    
    boolean accept( XLSRow row ) ;
}
