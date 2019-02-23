# Paxos

## project setup for development

just execute the following

```
git clone https://github.com/Prosdino95/Paxos.git
gradle eclipse
```

## How network stack works

### JSON message format

``` json
{
	RECIPIENTID : UUID<Long>,
	SENDERID    : UUID<Long>,
	VALUE       : Paxos value<Integer>,
	AGENTTYPE   : Type of agent to which this message is directed<String>
	MSGTYPE     : Type of message<String>
	FORWARDTYPE : Tells if this message must be send in broadcast or not (this information is never send on LAN)<String>
}
```

`


