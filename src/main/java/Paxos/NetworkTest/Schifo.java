package Paxos.NetworkTest;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

class Coda {

    ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue();


    Integer dequeue(){
       return q.poll();
    }

    void enqueue(int n){
        q.add(n);
    }

    int size(){
        return q.size();
    }
}


class add implements Runnable{

    Coda q;

    add(Coda q){
        this.q = q;
    }

    @Override
    public void run(){
        while(true){
            try {
                Thread.sleep(100);
                q.enqueue(1);
            }catch(Exception e){}

        }
    }
}

class rem implements Runnable{

    Coda q;

    rem(Coda q){
        this.q = q;
    }

    @Override
    public void run(){
        while(true){
            try {
                Thread.sleep(10);
                System.out.println(q.size());
                q.dequeue();
            }catch(Exception e){}

        }
    }
}

public class Schifo{

    public static void main(String[] args){
        Coda q = new Coda();
        add a = new add(q);
        rem r = new rem(q);

        Thread t1 = new Thread(a);
        Thread t2 = new Thread(r);

        t1.start();
        t2.start();

    }
}