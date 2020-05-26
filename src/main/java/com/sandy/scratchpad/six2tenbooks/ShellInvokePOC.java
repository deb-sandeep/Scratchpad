package com.sandy.scratchpad.six2tenbooks ;

import java.io.BufferedReader ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.List ;

public class ShellInvokePOC {
    public static void main( String[] args ) {
        Process p ;
        try {
            List<String> cmdList = new ArrayList<String>();
            cmdList.add( "sh" ) ;
            cmdList.add( "/home/sandeep/projects/source/ShellScripts/vi-imagify.sh" ) ;
            cmdList.add( "param1" ) ;
            cmdList.add( "param2" ) ;
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            p = pb.start();
                
            p.waitFor(); 
            BufferedReader reader = new BufferedReader( new InputStreamReader(
                                                      p.getInputStream())); 
            String line; 
            while((line = reader.readLine()) != null) { 
                System.out.println(line);
            } 
        }
        catch( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
        catch( InterruptedException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
    }
}
