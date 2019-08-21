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
java -jar build/libs/Paxos-Network.jar <-n>
```

The -n flag indicates that the node will host the name server. Only one name server can be active on the network, so the first node launched with such flag will host the name server (subsequent activations of the stack with the -n flag on different machines won't cause a name server to be launched, but the location of the name server is dynamically performed)

The network is also able to dynamically discover where the name server resides, if no argument is supplied to the program, that is if the network is started with:

```
java -jar build/libs/Paxos-Network.jar
```

The way the network stack perform such discovery is really simple: when the stack is launched, a `NameProber` thread is activated. Such thread waits for UDP messages on port 40001. When the network is raised, the prober sends in broadcast (truly network broadcast, that is on IP address 192.168.1.255) a `WHEREISNAMING` message. If there is a running node on network, its `NameProber` will respond to such request with a `NAMINGAT` message, reporting the IP address of the machine actually hosting the name server. Until a `NAMINGAT` message is received, the client waits for a name server to be activated (manually or as a result of a name server recovery), before join the network.

The network is also able to dynamically discover where the name server resides, if no IP for the name server is supplied, that is if the network is started as:

```
java -jar build/libs/Paxos-Network.jar
```

The way the network stack perform this is really simple: when the program is launched, it also launch a `NameProber` thread. Such thread waits for UDP messages on port 40001. When the network is raised, the prober sends in broadcast (truly network broadcast, that is on IP address 192.168.1.255) a `WHEREISNAMING` message. If there is a running node on network, its `NameProber` will respond to such request with a `NAMINGAT` message, reporting the IP address of the machine actually hosting the name server. If no response is received before a given timeout, than the node just brought up is the only one on network, and it will host the name server (until it fails). Then it will respond to future `WHEREISNAMING` requests with its own IP.

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

``` java
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

In order to properly work on LAN, a naming service must be present on the network. As known, when a new physical node connects for the first time to the network, it has no idea about the presence of other running machines. This translates to the practical impossibility to perform any remote communication. From here the need of a service able to keep track of the currently active nodes on network. The name service is started as explained before. The class `NamingRequestHandler` is responsable to handle all naming requests. When a `NAMINGREQUEST` message is processed by the name server, it replies back with the list of known running physical nodes. Each phyisical node is identified by its IP address, which obviously is unique in a LAN setting. New nodes subscribes to the naming service by means of a `NAMINGUPDATE` message.

The nice fact is that the name server is totally equivalent to any other LocalNetworkProcess instances, than there is no need inside the network stack to special structures for it. When the name server is launched, it is simply attached to the local network infrastructure which starts forwarding the traffic to it. If the name service resides on a remote node, the network infrastructure forwards the traffic to the physical machine where it is running which in turn will route the traffic to the name server. As a result, there is no difference between a network infrasturcture which hosts the name server and one that not, the only difference is that the machine hosting the name server will have, as a consequence, a running thread more.

#### Discovering processes on network

As stated before, the network stack allows every process to be informed about the precence of currently active processes on network. This is implemented by a set of `DISCOVER` messages. First of all, in order to avoid ambiguity the stack requires that every process has a unique identifier. Here unique must be intended on an entire network scale, and not only on a local machine basis. If two processes have the same identifier, the one coming for last won't be actually considered, since there exists another running process with the same identifier. The stack gives for guaranteed this fact (i.e. is responsibility of the consumer to guarantee the unique identifiability of each process). 

When a process connects, the first thing it does is sending a `SUBSCRIBE` message to the local network infrastucture to notify its presence. Morover the `SUBSCRIBE` is sent in broadcast to all network infrastructures, this to guarantee that new connected processes are immediately visible from other ones, even if a `DISCOVER` has not been started yet. Furthermore, suppose that a new process connects to the network, starts a `DISCOVER` and then sends a message to a certain process. What happens is that the recipient of the message is not able to respond to the new process if the bind [UUID-socket] is not known by the network infrastructure; the problem clearly rises from the fact that the new process knows about the remote node, but the opposite is not true. Sending the `SUBSCRIBE` in broadcast when a process connects to the network is enought to avoid the problem.

Clearly a `DISCOVER` mechanism is still necessary, since allows to update the vision of the network (otherwise nodes which leaves the network are not seen, since if a process fails, it do it silently, without informing other processes is going to fail).
When a process wants to be informed about the precence of currently active processes on network it sends a `DISCOVERREQUEST` message to the local infrastructure. The call is **synchronous**, in the sense that the caller is blocked until **all known physical nodes have replied**, this guarantees that when the control is given back to the caller, it has the most updated state of the network.
In response to a `DISCOVERREQUEST`, the local infrastructure immediately replies with the list of local processes and then starts sending `DISCOVER` messages to all known remote nodes, in order to be aware also on processes running on remote machines. 
When a remote network infrastrucure receives a `DISCOVER` message it responds directly with a `DISCOVERREPLY` containing the list of its local active processes to the sender of the `DISCOVER`. In turn, the network infrastructure which originated the `DISCOVER` on reception of the `DISCOVERREPLY` directly forwards it to the process which started the request. 
Note that here the network stack performs some extra-processing  before forwarding the message. Indeed it makes a diff between the processes currently considered active and the ones contained in the `DISCOVERREPLY`, if the diff mismatch the network infrastructure removes death associations from `SocketRegistry` or conversely adds in the registry a new association [UUID - socket] for all unknown identifiers.

At the end, when all the `DISCOVERREPLY` have been received, who started the `DISCOVER` has the list of all known processes running on the network.

Note that such mechanism is offered by `NetworkInterface` under the method `updateConnectedProcessList()`. Furthermore the method before sending the `DISCOVERREQUEST` forces also a naming lookup to get the IPs of all connected phyiscal nodes, since processes may have a not updated vision of the network.

#### Keeping track of active nodes on network

We know, processes or even entire nodes can fail. The network must be able to react to these failures. The reason why we need such mechanism is for example the need for processes to have a list of active processes, then without reporting processes which are not running anymore. Another reason arises from the fact that the DISCOVER mechanism is synchronous, then if an entire physical machine fails, a process which started a DISCOVER before the machine failure was known will be blocked forever, waiting for a response from a death node. Then processes must be informed about such kind of events.

The class `Tracker` is responsable to track local active processes. Each network infrastructure has a Tracker thread periodically sending `PING` messages to local processes. The process is considered alive if a response to the `PING` message is received before a given timeout. When a `PING` is received, any previously `PING` which has not been acked is considered indeed acked, since a response to a later `PING` has been received, and than the process (or the remote node) is still alive. Otherwise the process is considered death and its identifier is removed from the list of known active processes. Then next DISCOVER won't report it as an active process.

To avoid useless traffic generated from nodes not hosting the name server, the detection of death physical nodes is performed by `NamingRequestHandler`, which indeed is the only interested in updating the list of kwnown active IPs. Like the tracker class, the name service periodically sends `PING` messages, but in this case to all known remote network infrastructures. Again these are considered alive if the response is received before a given timeout.
On the contrary, if a response is not received, the node is considered death, as well as all its processes. The name server removes its reference from the list of known IPs, and then signals this event by sending a `SIGUNLOCK` in broadcast. Such message has as effect to unlock any process blocked on a DISCOVER because waiting for a response from a death node.

Next time a process starts a DISCOVER, the naming request generated as a consequence will return the correct list of active nodes, avoiding the problem, and not reporting as active all processes which were hosted by the death node.

#### Name service fault recovery

Name server is a crucial point for the entire infrastructure. If such process is not able to respond, no response to a `NAMINGREQUEST` will be received, causing the entire DISCOVER mechanism to fail. Furthermore, the fact that new nodes can't subscribe to the network, make them to be ignored by all other active nodes. 
In other words the naming service constitutes a single point of failure: if it fails, the entire infrastructure fails too. For this reason we need a way to recover the system in case of naming service faults. The way this network stack performs this is with the well known bully-election algorithm.

Every node processing a `NAMINGREQUEST` before forwarding it to the name server, issues a ticket to track the ongoing request. If a response to such request is not received before a given timeout, the name server is considered death, and a bully election is started. The node which for first discovers the naming failure sends to all other nodes having an UUID greater than its an `ELECT` message, issuing a new ticket to keep track of the ongoing election. On reception of an `ELECT` message, if the UUID of the recipient is greater than the one of the sender, it sends back to the sender a `BULLYSUPPRESS` message, by which the sender of the `ELECT` removes any ticket issued as a consequence of the `ELECT` transmission.
The only node which will have a ticket expired, will be the one having the greatest UUID on the network, since it's the only node which hasn't received a `BULLYSUPPRESS` (hence its ticket has not been removed). Such node has just been elected as the new name server. It informs all other nodes about this, by broadcasting a `COORD` message. In turn, when a node receives such `COORD` message, it updates its name server reference by making it to point to the node hosting the new name server.
On the elected node, a new instance of `NamingRequestHandler` is launched, ready to serve naming requests as the old one. The list of active nodes is restored from the last `NAMINGREPLY` received, before the failure was detected.
Note that during this critical state of the network, all traffic not related to the name recovery is not accepted (the messages are simply dropped by the infrastructure) and no nodes can join the network (they are halted until the recovery of the naming server has been performed; when the name server is again up, the `NAMINGAT` response is actually sent and the new node can join the network). 
This simplifies the infrastructure, since avoids tricky race conditions which may happen if we allow new nodes to join the network during such a critical moment. Furthermore, the election is guaranteed to be performed by the only alive nodes present on the network at the moment of the failure, and hence as a consequence the new name server will run on one of these nodes.
 
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
	MACHINEUUID : UUID for the machine, required by fault tolerance purposes
}
```

Informations about the sender ID, the IP address and the type of forwarding are inserted by the network stack, as well as the machine UUID. To force the broadcast transmission of a message, the method `.setAsBroadcast()` must be called by the sender on the specific message for which the broadcast transmission is required (before calling `.getJSON()`).

Internal messages, such the ones required for the naming service, the tracker or the DISCOVER mechanism have a special structure. Many of them are composed by the only MSGTYPE field, others needs to carry special informations. Neverthless consumers never sees such messages, but only the ones having a MSGTYPE field which does not refer to any internal function. Such messages can be taken and processed by the consumer as it wants.

We report a list of all internal messages used by the network stack:

| Message | Description |
| --- | --- |
| SUBSCRIBE | First message sent by `LocalNetworkProcess` to the local network infrastructure to bind the UUID of the process with the socket such process uses to communicate (returned by `ConnectionHandler`). Furthermore the message is forward to all remote nodes which in turn keep track of the association [process UUID - remote node socket behind which the process is running] |
| DISCOVERREQUEST | Sent by `LocalNetworkProcess` to start a DISCOVER. The local network infrastructure replies back with the list of local processes and then sends in broadcast a `DISCOVER` message |
| DISCOVERREPLY | Sent as a response to a `DISCOVER`, contains a list with the UUID of all processes considered alive on the remote machine. When received, causes the network infrastructure to update the associations [UUID - socket] contained in `SocketRegistry` and notifies the process waiting for a response from the remote machine that the reply has been received |
| DISCOVER | When received by a remote node, causes as a response a `DISCOVERREPLY` |
| NAMINGSUBSCRIBE | Sent by the name server, is required to bind the socket used by the name server inside the `SocketRegistry`, in this way the network infrastructure is able to forward traffic to it |
| NAMINGUPDATE | Used to register an IP address to the list of connected pyhisical nodes. |
| NAMINGREQUEST | Sent when a process requires an update of the current connected machines to the network. Causes the name server to reply with a `NAMINGREPLY` |
| NAMINGREPLY | Contains the list of all known IPs connected to the network. Furthermore the network infrastructure, before route the message, opens a connection with new remote nodes and removes inactive associations (association with nodes not reported as alive) |
| PING | Used by `Tracker` or any other tracking function inside the stack for probing if the recipient is alive or not. Any `PING` message is characterized by a randomly choosen value which identifies the message. A response to a `PING` message is still a `PING` message containing exactly the same random value. When a `PING` is received, the tracker is informed about the response and can considered acked the message sent with that specific random value |
| SIGUNLOCK | Sent by the `NameRequestHandler` when a remote node is not able to responde to `PING` requests to unlock any process on network still waiting for a response from that death node |
| WHEREISNAMING | Sent by `NameProber` on UDP broadcast to dynamically discover the IP address of the node where the name server is currently running |
| NAMINGAT | Sent by `NameProber` as response to a `WHEREISNAMING` message containing the IP address of the node where the name server is running. The recipient can in this way keep track of this reference and sent its naming requests to it |

Any other message is not part of the network stack and is simply routed according to the RECIPIENTID field and the FORWARDTYPE one. This means that the stack does not perform any kind of extra processing. To add custom messages, add in the enum `MessageType` something like the following:

``` java
MESSAGENAME("MESSAGENAME", (s,m) -> MessageType.forwardTo(s,m))
```

consumer will then be able to send and receive messages with that kind of identifier.



## Copyright

All code was witten by:
Alessandro Palummo https://github.com/AlePalu,
Marco Prosdocimi.
For Distributed System project,
Politecnico di Milano 2019
