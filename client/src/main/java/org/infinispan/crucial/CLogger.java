package org.infinispan.crucial;

/**
 * @author Daniel
 */
public class CLogger{
    private final String name;

    public CLogger(String name){
        this.name = name;
    }

    public void print(String out){
        System.out.println("["+name+"-log] " + out);
    }
}
