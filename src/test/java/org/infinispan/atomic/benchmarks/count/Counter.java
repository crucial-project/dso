package org.infinispan.atomic.benchmarks.count;

/**
 *  The <tt>Counter</tt> class is a mutable data type to encapsulate a counter.
 *  <p>
 *  For additional documentation, see <a href="/algs4/12oop">Section 1.2</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

@Distributed
public class Counter implements Comparable<Counter> {

   @Key
   public String name;     // counter name
   public int count = 0;         // current value

   public Counter(){}

   /**
    * Initializes a new counter starting at 0, with the given id.
    * @param id the name of the counter
    */
   public Counter(String id) {
      name = id;
   }

   /**
    * Increments the counter by 1.
    */
   public void increment() {
      count++;
   }

   /**
    * The current count.
    */
   public int tally() {
      return count;
   }

   /**
    * A string representation of this counter.
    */
   public String toString() {
      return name + ":" + count;
   }

   /**
    * Compares this counter to that counter.
    */
   public int compareTo(Counter that) {
      if      (this.count < that.count) return -1;
      else if (this.count > that.count) return +1;
      else                              return  0;
   }

}
