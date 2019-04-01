# Paxos

## project setup

just execute the following

```
git clone https://github.com/Prosdino95/Paxos.git
gradle eclipse
```

## run Paxos

First build the project

```
gradle NetworkJar
```

To start the network infrastructure:

```
java -jar build/libs/Paxos-Network.jar <NameServerIP>
```

where the NameServerIP parameter is the IP of the phyisical machine where the naming service will run. If the IP matches the one of the machine where the command is launched, then the code will automatically launch the name server. Otherwise, the network stack will use the supplied IP as reference for the name server.

To start a client:

```
java -jar build/libs/Paxos-NetworkTest.jar
```


## How network stack works

The network is composed by two main components, a network infrastructure, unique for every phisical machine and responsable to handle the traffic for that node, and a set of network processes, offering the network interface to processes which requires to comminucate over the network. Every process willing to communicate on the network must have a corresponding network process attached to it, then from a network point of view there is no idea of the specific implementation of the process, nor of the type of messages such process need to process.

### The process view of the network

There is a unique point of access for a process to the network. This is offered by the interface `NetworkInterface`. NetworkInterface defines primitives to send, receive and to start a DISCOVER, a mechanism allowing the process to be informed about all processes running on the network.
The latter works under the assumption that all processes on network can be uniquely identified by some identifier.

`NetworkInterface` is implemented by `LocalNetworkProcess`. All a process has to do in order to interact with the network is the following:

```
String myIP = InetAddress.getLocalHost().getHostAddress();
NetworkInterface myProcess = new LocalNetworkProcess(myIP, 40000, processUUID);
Thread netThread = new Thread(myProcess);
netThread.start();
```

After this, all primitives offered by NetworkInterface can be accessed. LocalNetworkProcess will take care of the rest, giving to the consumer a nice abstraction of the network.

`LocalNetworkProcess` simply starts a thread handling both the inbound and outbound queues for the process, besides be responsable for the DISCOVER mechanism. All stuffs related to naming, communication facilities, failure detection are transparent to the consumer and are a responsibility of the network infrastructure.

### The network infrastructure

The network infrastructure is based on three main classes: 
* `ConnectionHandler`, listening on port 40000 and responsable to open new connections coming from both internal and external processes,
* `TrafficHandler`, which effectively routes the traffic between processes and applies some processing required by the network stack to execute its internal functions,
* `SocketRegistry`, which keeps track of all (process identifier - socket) associations as well as the name server socket and a list of remote node references.

Upon these three classes, which are enought to guarantee a basic network infrastructure able to send and receive messages, the network offer some more functionalities:

#### Name Service

In order to properly work on LAN, a naming service must be present on the network. As known, when a new physical node connects for the first time to the network, it has no idea about the presence of other running machines. This translates to the practical impossibility to perform any remote communication. From here the need of a service able to keep track of the currently active nodes on network. The class `NamingRequestHandler` is responsable to handle all naming requests. When a `NAMINGREQUEST` message is processed by the name server, it replies back with the list of known running physical nodes. Each phyisical node is identified by its IP address, which obviously is unique in a LAN setting. New nodes subscribes to the naming service by means of a `NAMINGUPDATE` message.

The nice fact is that the name server is totally equivalent to any other LocalNetworkProcess instance, than there is no need inside the network stack to special structures for it. When the name server is launched, it is simply attached to the local network infrastructure which starts forwarding the traffic to it. If the name service resides on a remote node, the network infrastructure forwards the traffic to the physical machine where it is running which in turn will route the traffic to the name server. As a result, there is no difference between a network infrasturcture which hosts the name server and one that not, the only difference is that the machine hosting the name server will have, as a consequence, a running thread more.

#### Discovering processes on network

As stated before, the network stack allows every process to be informed about the precence of currently active processes on network. This is implemented by a set of `DISCOVER` messages. First of all, in order to avoid ambiguity the stack requires that every process has a unique identifier. Here unique must be intended on a entire network scale, and not only on a local machine basis. If two processes have the same identifier, the one coming for last won't be actually considered, since there exists another running process with the same identifier. The stack gives for guaranteed this fact (i.e. is responsibility of the consumer to guarantee the unique identifiability of each process). 

When a process connects, the first thing it does is sending a `SUBSCRIBE` message to the local network infrastucture to notify its presence. Morover the `SUBSCRIBE` is sent in broadcast to all network infrastructures, this to guarantee that new connected processes are immediately visible from other ones, even if a `DISCOVER` has not been started yet. Furthermore, suppose that a new process connects to the network, starts a DISCOVER and then sends a message to a certain process. What happens is that the recipient of the message is not able to respond to the new process if the bind [UUID-socket] is not known by the network infrastructure; the problem clearly rises from the fact that the new node has the most updated state of the network, but the old one not, than it doesn't know about the new process. Sending the `SUBSCRIBE` in broadcast when a process connects to the network is enought to avoid the problem.

Clearly a `DISCOVER` is still necessary, since allow for update the vision of the network (nodes which leaves the network are not seen).
When a process wants to be informed about the precence of currently active processes on network it sends a `DISCOVERREQUEST` message to the local infrastructure. The call is synchronous, in the sense that the caller is blocked until all physical node have replied, this guarantees that when the control is given back to the caller, it has the most updated state of the network. In response to a `DISCOVERREQUEST`, the local infrastructure immediately replies with the list of local processes and then starts sending `DISCOVER` messages to all known remote nodes, in order to be aware also on processes running on remote machines. 
When a remote network infrastrucure receives a `DISCOVER` message it responds directly with a `DISCOVERREPLY` containing the list of its local active processes to the sender of the `DISCOVER`. In turn, the network infrastructure which originated the `DISCOVER` on reception of the `DISCOVERREPLY` directly forwards it to the process which started the request. At the end, when all the `DISCOVERREPLY` have been received, such process has the list of all known processes running on the network.

#### Keeping track of active nodes on network

We know, processes or even entire nodes can fail. The network must be able to react to these failures. The reason why we need such mechanism is for example the need for processes to have a list of active processes, then without reporting processes which are not running anymore. Another reason arises from the fact that DISCOVER mechanism is synchronous, then if an entire physical machine fails, a process which started a DISCOVER before the machine failure was known will be blocked forever, waiting for a response from a death node. Then processes must be informed about such kind of events.

The class `Tracker` is responsable to track local active processes. Each network infrastructure has a Tracker thread periodically sending `PING` messages to local processes. The process is considered alive if a response to the PING message is received before a given timeout. Otherwise the process is considered death and its identifier is removed from the list of known active processes. Then next DISCOVERs won't report it as an active process.

To avoid useless traffic generated from nodes not hosting the name server, the detection of death physical nodes is performed by `NamingRequestHandler`, which indeed is the only interested in updating the list of kwnown active IPs. Like the tracker class, the name service periodically sends PING messages, but in this case to all known remote network infrastructure. Again these are considered alive if the response is received before a given timeout. 
On the contrary, if a response is not received, the node is considered death, as well as all its processes. The name server removes its reference from the list of known IPs, and then signals this event by sending a `DISCOVERKILL` in broadcast. Such message has as effect to unlock any process blocked on a DISCOVER because waiting for a response from a death node.

Next time a process starts a DISCOVER, the naming request generated as a consequence will return the correct list of active nodes, avoiding the problem, and not reporting as active all processes which were hosted by the death node.

### Message format

Although the network stack sends and receives strings, in order to build protocols which are usable we need some string formatting. This network stack requires that string exchanged by processes are formatted in JSON. To ease the work to consumers, a class `Message` is offered. `Message` simply offers a way to build messages without think to its specific JSON structure. 

Create a message is as simply as follow:

``` java
Message msg = new Message(recipientUUID, payload, messageType);
```

The method `.getJSON()` automatically generates the corresponding string formatted in JSON, ready to be sent by the network interface.
When a message leaves the network interface, it is structured as follow (i.e. this is what is seen by the network infrastructure):
```
{
	RECIPIENTID : UUID<Long>,
	SENDERID    : UUID<Long>,
	VALUE       : payload<String>,
	MSGTYPE     : Type of message<String>
	FORWARDTYPE : Type of forwarding (broadcast/unicast)<String>
	NAME        : IP address of the sender<String>
}
```

Informations about the sender ID, the IP address and the type of forwarding are inserted by the network stack. To force the broadcast transmission of a message, the method `.setAsBroadcast()` must be called by the sender on the specific message for which the broadcast transmission is required (before calling `.getJSON()`).

Internal messages, such the ones required for the naming service, the tracker or the DISCOVER mechanism have a special structure. Many of them are composed by the only MSGTYPE field, others needs to carry special informations. Neverthless consumers never sees such messages, but only the ones built by `Message` constructor.

