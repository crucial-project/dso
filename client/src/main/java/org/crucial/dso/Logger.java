package org.crucial.dso;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Daniel
 */

@Entity
public class Logger {

    @Id
    public String name;

    public Logger() {
    }

    public Logger(String name) {
        this.name = name;
    }

    public void print(String out) {
        System.out.println("[" + name + "-log] " + out);
    }

}