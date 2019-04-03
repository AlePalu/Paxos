package Paxos.Agents;

public class PaxosData {

    private String currentValue;
    private int numOfProces;
    private long id;
    private int round;
    private boolean win;

    public PaxosData(int numOfProces, long id){
        this.currentValue= null;
        this.numOfProces = numOfProces;
        this.id=id;
        this.round = 0;
        this.win = false;

    }

    void reset(){
        this.currentValue = null;
        this.win = false;
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

    void nextRound(){
        this.round++;
    }

    int getRound(){
        return this.round;
    }

    boolean isWin(){
        return win;
    }

    void setwin(){
        win = true;
    }
}
