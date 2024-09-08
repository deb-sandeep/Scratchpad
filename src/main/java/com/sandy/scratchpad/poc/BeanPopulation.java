package com.sandy.scratchpad.poc;

import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

@Data
class Client {
    private String name = null ;
    
    public Client() {
        name = "Sandeep" ;
    }
}

@Data
public class BeanPopulation {
    
    private List<Client> clients = new ArrayList<>();
    
    public static void main( String[] args ) throws Exception {
        BeanPopulation obj = new BeanPopulation() ;
        PropertyUtils.setIndexedProperty( obj, "clients[0]", new Client() );
        System.out.println( obj.clients.get( 0 ).getName() ) ;
    }
}
