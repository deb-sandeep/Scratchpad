package com.sandy.scratchpad.evtdoc.collector;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public abstract class BaseEventCollector extends VoidVisitorAdapter<Void> {
    
    protected String curPkgName = null ;
    protected String curClsName = null ;
    protected String curMthName = null ;
    
    @Override
    public void visit( PackageDeclaration n, Void arg ) {
        this.curPkgName = n.getNameAsString() ;
        super.visit( n, arg );
    }
    
    @Override
    public void visit( ClassOrInterfaceDeclaration n, Void arg ) {
        this.curClsName = n.getNameAsString() ;
        super.visit( n, arg );
    }
    
    @Override
    public void visit( MethodDeclaration n, Void arg ) {
        this.curMthName = n.getNameAsString() ;
        super.visit( n, arg ) ;
    }
}
