# Paxos

## project setup for development

just execute the following

```
git clone https://github.com/Prosdino95/Paxos.git
gradle eclipse
```

## How network stack works

network is organized by two main components, a network infrastructure, unique for every phisical machine and responsable to handle the traffic for that node, and a set of network interfaces, backed by some process. Every process willing to communicate on the network must have a corresponding network interface attached to it, then from a network point of view there is no idea of the specific implementation of the process, nor of the type of messages such process need to process.

The network infrastructure is based on two main classes: ConnectionHandler, responsable to handle new connections coming from some internal or external process, and TrafficHandler, which effectively routes the traffic between processes.

From an high level point of view, the network offers primitives to send and receive messages, both in broadcast and in unicast, and allows to know what process is running on the network. As a result every process on network knows every other process on the network.

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


