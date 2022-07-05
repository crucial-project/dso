package org.crucial.dso.dining;

import org.crucial.dso.Factory;

import java.util.ArrayList;
import java.util.List;

public class Dine {

    public static void main(String[] args) {

        String address = args.length> 0 ? args[0] : "127.0.0.1:11222";

        Factory.get(address);

        int x = 10;

        Log.msg(String.valueOf(x));

        Chopstick[] chopsticks = new Chopstick[5];

        //initialize the chopsticks
        for (int i = 0; i < chopsticks.length; i++) {
            chopsticks[i] = new Chopstick("C: " + i);
        }
        Philosopher[] philosophers = new Philosopher[5];
        //for(i=0; i<philosophers.length; i++){
        philosophers[0] = new Philosopher("P: 0 - ", chopsticks[0], chopsticks[1]);
        philosophers[1] = new Philosopher("P: 1 - ", chopsticks[1], chopsticks[2]);
        philosophers[2] = new Philosopher("P: 2 - ", chopsticks[2], chopsticks[3]);
        philosophers[3] = new Philosopher("P: 3 - ", chopsticks[3], chopsticks[4]);
        philosophers[4] = new Philosopher("P: 4 - ", chopsticks[4], chopsticks[0]);

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < philosophers.length; i++) {
            Log.msg("Thread " + i);
            Thread t = new Thread(philosophers[i]);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Log.msg(e);
            }
        }

        System.exit(0);

    }
}

// State : 2 = Eat, 1 = think
class Philosopher extends Thread {
    private Chopstick _leftChopistick;
    private Chopstick _rightChopistick;

    private String _name;
    private int _state;

    public Philosopher(String name, Chopstick _left, Chopstick _right) {
        this._state = 1;
        this._name = name;
        _leftChopistick = _left;
        _rightChopistick = _right;
    }

    public void eat() {
        boolean hasEaten = false;
        while (!hasEaten) {
            if (_rightChopistick.take()) {
                if (_leftChopistick.take()) {
                    Log.msg(_name + " : Eat");
                    hasEaten = true;
                    _leftChopistick.release();
                }
                _rightChopistick.release();
            }
        }
        think();
    }

    public void think() {
        this._state = 1;
        Log.msg(_name + " : Think");
    }

    public void run() {
        for (int i = 0; i <= 10; i++) {
            eat();
        }
    }
}

class Log {

    public static void msg(String msg) {
        System.out.println(msg);
    }

    public static void msg(Throwable t) {
        System.err.println(t.getMessage());
        t.printStackTrace();
    }
}
