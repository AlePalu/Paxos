interface NetworkManagerInterface{
    public void enqueueMaessage(Message msg);
    public Message dequeueMessage();
    public Boolean isThereAnyMessage();
    public void subscribe();
}
