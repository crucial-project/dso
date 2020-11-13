package org.crucial.dso;

import java.io.Serializable;

/**
 * From "Two Algorithms for Barrier Synchronization", Hensgen et al.
 */
public class ScalableCyclicBarrier {

    private static final int SLEEP = 1;

    private final AtomicBoolean answers[][];
    private final int parties, logParties;

    private AtomicCounter identity;
    private ThreadLocal<Integer> myIdentifier;

    public ScalableCyclicBarrier(final String name, final int parties, AtomicBoolean[][] answers, AtomicCounter identity){
        this.parties = parties;
        this.logParties = (int)(Math.log(parties)/Math.log(2));
        this.answers = answers;
        assert answers.length == parties;
        for(int p=0; p<parties; p++) {
            assert answers[p].length == logParties;
        }
        this.identity = identity;
        this.myIdentifier = new ThreadLocal<>();
    }

    public int await(){
        if (this.myIdentifier.get()==null) this.myIdentifier.set(identity.increment());
        int myId = this.myIdentifier.get();

        int intended[] = new int[logParties];
        int power = 1;
        for(int i=0; i<logParties; i++){
            intended[i] = (power + myId) % parties;
            power = power * 2;
        }

        try{
            for(int instance=0; instance<logParties; instance++) {
                if (intended[instance] == myId) continue;
                while (answers[intended[instance]][instance].get()) {Thread.currentThread().sleep(SLEEP);}
                answers[intended[instance]][instance].set(true);
                while (!answers[myId][instance].get()) {Thread.currentThread().sleep(SLEEP);}
                answers[myId][instance].set(false);
            }
        } catch (InterruptedException e) {
            // ignore
        }

        return 0;
    }

}
