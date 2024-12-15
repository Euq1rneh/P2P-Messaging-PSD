# Data Privacy and Security Project

## Course: MSI/MEI 24/25

### Project Overview

This project involves implementing a peer to peer messaging system using The project is divided into multiple phases:

- **Phase 1**: Basic implementation of the app with message encryption.
- **Phase 2**: Enhance security and privacy and choose between some tasks. This project implements the long-term storage and searchable encryption tasks

The schemes chosen were Shamir Secret Sharing and Cash Searchable Encryption

---

### Project Structure
Base Packages
- **client**: contains classes for the clients to be able to comunicate securely with the servers and other clients
- **server**: contains classes for server behaviour enabling message searching and file backup

Important Classes
- **client.peer.Peer**: contains almost all the important behaviour for the clients
- **server.messages.MessageReader**: contains behaviour for processing client requests
---

### Files in This Repository

- **src/**: Source code files for the project.
- **dist/**: Directory for the compiled executables.
- **docs/**: Directory containing project-related documentation.
- **build.sh**: Script to compile all source files into executables (client and servers) also creates the necessary file structure to run the executables.
- **config.ini**: Configuration file for each client and server. Contains the necessary information to start each executable
---

### User and Server information
Available users
```
- fc11111
    Keystore: fc11111-keystore.jceks
    Password: fc11111
    Truststore: truststore.jceks (does not have a password)
- fc22222
    Keystore: fc22222-keystore.jceks
    Password: fc22222
    Truststore: truststore.jceks (does not have a password)
- fc33333
    Keystore: fc33333-keystore.jceks
    Password: fc33333
    Truststore: truststore.jceks (does not have a password)
- fc44444
    Keystore: fc44444-keystore.jceks
    Password: fc44444
    Truststore: truststore.jceks (does not have a password)
```
Available Server
```
- amazonServer
    Keystore: amazonServer-keystore.jceks
    Password: amazonServer
    Truststore: truststore.jceks (does not have a password)
    Port: 1111 (PORT MUST BE THE SAME)
- googleServer
    Keystore: googleServer-keystore.jceks
    Password: googleServer
    Truststore: truststore.jceks (does not have a password)
    Port: 3333 (PORT MUST BE THE SAME)
- oracleServer
    Keystore: oracleServer-keystore.jceks
    Password: oracleServer
    Truststore: truststore.jceks (does not have a password)
    Port: 2222 (MUST BE THE SAME)
```

### Installation and Setup
Before starting make sure that the keystores and truststores directories have keystores in them or the program will not start

#### 1. Run the build script in the root of the project
```
./build.sh
```
#### 2. Open as many terminals as there will be clients (max of 4) and at least 2 terminals for the servers (max of 3)
#### 3. In each terminal cd into one of the directories in the dist directory, the compilation script will have created a folder structure that looks like this
```
dist
  amazonServer
    keystore
      amazonServer-keystore.jceks
    truststore
      truststore.jceks
    messagingAppServer.jar
  fc11111
    keystore
    truststore
    messagingAppPeer.jar
  fc22222
    ...
  fc33333
    ...
  fc44444
    ...
  googleServer
    ...
  oracleServer
    ...
```
#### 3. For each executable create a `config.ini` file like replacing the fields with appropriate values (some of the values are written above)
```
keystorePath=keystore/amazonServer-keystore.jceks
keystorePassword=amazonServer
truststorePath=truststore/truststore.jceks
truststorePassword=
username=amazonServer
port=1111
```
#### 4. Run executable (Servers must be executed first)
For servers
```
java -jar messagingAppServer.jar
```
For clients
```
java -jar messaginAppPeer.jar
```
### Available Commands (client only, servers do not respond to any terminal commands)
- **:t ip port alias** - this command allows a peer to send a message to another. The parameters are the ip and port of the peer and the alias of the peer (alias in the truststore where the certificate of that peer is)  
**:b** - this command can only be used after the :t command and allows the user to go back to the main menu
- **:o conversation_id** - opens a conversation with id conversation_id  (the id is the number presented before the name of the conversation)
- **:s keyword** - searches for a keyword in the conversations files and returns the name of the files that contain that word  
- **:q** - this command allows a user to quit the app  

