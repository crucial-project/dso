package org.infinispan.crucial.benchmarks.dining;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class Chopstick implements Serializable{

   public boolean used;

   @Id
   public String _name;

   public Chopstick(String _name){
      this._name = _name;
   }

   public synchronized boolean take() {
      Log.msg ("Used :: " + _name );
      if (this.used) return false;
      this.used = true;
      return true;
   }
   public synchronized void release() {
      Log.msg ("Released :: " + _name );
      this.used = false ;
   }
}
