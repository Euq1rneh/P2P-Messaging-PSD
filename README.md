# P2P Messaging App
Data Privacy and Security Project
Uses java 21

## Commands
  - **:q** - this command allows a user to quit the app
  - **:t ip port alias** - this command allows a peer to send a     message to another. The parameters are the ip and port of the peer and the alias of the peer (alias in the truststore where the certificate of that peer is)
  - **:o conversation_id** - opens a conversation with id conversation_id
  - **:b** - this command can only be used after the **:t command** and allows the user to go back to the main menu

### Available users
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



### Functional Examples
For user fc11111 (assuming there is at least another user)  

To talk to a peer try the command below:  

    :t 127.0.0.1 22222 fc22222

The following menu would appear

    ---------- Messaging fc22222 ----------
    Message > Hello
    Message > 

Write any message and send it, this will create a `.conversation` file starting with the name of the other peer. In this case 2 files would be created, one for each peer. For peer fc11111 the file `fc22222.conversation` and for peer fc22222 the file `fc11111.conversation`.

To return to the main menu just use the :b command (this will not send a message to the peer)


    ---------- Messaging fc22222 ----------
    Message > Hello
    Message > :b


To open the conversation you just created use the open command (assuming there is a conversation that can be opened):

    :o 0 (zero is the conversation id shown in the terminal)

The following is an example main menu

    ----------------- Conversations -----------------
    0. fc22222
    1. John Smith