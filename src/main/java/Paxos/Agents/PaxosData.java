package Paxos.Agents;


import java.util.Timer;

public class PaxosData {

    private String currentValue;
    private int numOfProces;
    private long id;
    private int round;
    private boolean proposewin;
    private boolean learnerwin;
    private Timer timer;
    private long acceptorBound;

    public PaxosData(int numOfProces, long id){
        this.currentValue= null;
        this.numOfProces = numOfProces;
        this.id=id;
        this.round = 0;
        this.proposewin=false;
        this.learnerwin=false;
        this.timer = new Timer();
        this.acceptorBound=0;

    }

    void reset(){
        this.currentValue = null;
        this.proposewin=false;
        this.learnerwin=false;
        this.timer.cancel();
        this.timer.purge();
        this.timer = new Timer();
        this.acceptorBound=0;
    }

    long getId() {
        return id;
    }

    void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    void setNumOfProces(int numOfProces) {
        this.numOfProces = numOfProces;
    }

    String getCurrentValue() {
        return currentValue;
    }

    int getNumOfProces() {
        return numOfProces;
    }


    void nextRound(){ this.round++; }

    int getRound(){
        return this.round;
    }

    long getAcceptorBound(){
        return this.acceptorBound;
    }

    void setAcceptorBound(long b){
        this.acceptorBound=b;
    }

    Timer getTimer(){
        return timer;
    }

    void setProposewin(){
        proposewin=true;
    }
    void setLearnerwin(){
        learnerwin=true;
    }

    boolean getProposewin(){
        return proposewin;
    }

    boolean getLearnerwin(){
        return learnerwin;
    }

}
