package org.infinispan.atomic.benchmarks.dining;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.io.Serializable;

@Distributed
public class Chopstick implements Serializable{

   public boolean used;
   @Key
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
