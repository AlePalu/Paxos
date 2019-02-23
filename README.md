# Paxos

## project setup for development

just execute the following

```
git clone https://github.com/Prosdino95/Paxos.git
gradle eclipse
```

## How network stack works

### JSON message format

```
{
	RECIPIENTID : UUID<Long>,
	SENDERID    : UUID<Long>,
	VALUE       : Paxos value<Integer>,
	AGENTTYPE   : Type of agent to which this message is directed<String>
	MSGTYPE     : Type of message<String>
	FORWARDTYPE : Type of forwarding (broadcast/unicast) (information never sent on LAN)<String>
}
```


