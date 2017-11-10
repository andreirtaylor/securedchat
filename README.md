# securedchat
Toy project to do do secure local messaging.

The server and client run in different processes. They need to be run in different
command line terminals. They use AES encryption to perform authentication although
this is easliy changed;

## Folder Permission
In order for the app to function, there needs to be some modification to the permission. 

Both the client and the server needs to have permission of the folder. In order to achieve this, run this command:

```bash
sudo chown -R $USER:$USER FOLDER_NAME
```

Both client and server needs to be added to a new group and the group needs to be granted permission for the folder.
To create a new group and add the client/server to the group, run:

```bash
sudo groupadd CLIENT_GROUP
sudo adduser CLIENT CLIENT_GROUP
```

```bash
sudo groupadd SERVER_GROUP
sudo adduser SERVER SERVER_GROUP
```

Last thing to add is the permission of the new group to access the folder.
To add the folder to be part of the new group, run:

```bash
chgrp groupA ./folderA
```

To grant the group permission to the folder, run:

```bash
chmod g+rwx  ./folderA
```
## How to run

Compile the client/server in the src folder

```bash
javac SecureChat.java Message.java MessageReader.java MessageWriter.java
```

then run the server with

```bash
java SecureChat --server
```

then run the client (in a separate terminal)

```bash
java SecureChat --client
```

## How it works

The chat application works by sending messages from client to server and vice versa.

![](assets/README-d5273.png)

The client must be commence the connection and once the server sees the connection a bidirectional channel is created between the client and the server.


### Security options

There are three independent options for security between the client and the server.

1. Confidentiality: The message and password (if selected below) are both encrypted when being sent between the client and server.
2. Integrety: If selected this will ensure a encrypted check sum is sent along with the message. Note that the encryption of this check sum is exclusively to validate that the client is the origin of the message. It is not related to the Confidentiality of the message in any way.
3. Authentication: if slected this will send a password as well as the message. This is encrypted only if the Confidentiality is selected above

These options are independent and must be agreed upon by both the client and the server. If there is not an agreement then no connection is made.

Note that due to the independent nature of the above options there is up to 7 different configurations of the security options

```text
None: Message is sent in plain text
C: Message is encrypted
I: Checksum is sent with message
A: Password is sent with message (Unencrypted)
CI: Message is sent encrypted with Checksum
CA: Message is sent encrypted, password is sent encrypted
IA: Checksum is sent with, Unencrypted message and password
CIA: Checksum is sent with encrypted message and password
```


### How messages are sent

The Client and server pass messages to eachother through files in protected folders. The server user is called {Add server here} and the client is called {Add client user here}. The permissions for these two users only allow the server to write to the *clientIncomming* folder without being able to read and the *serverIncomming* folder is able to be written by the client and read by the server.

When a message is sent to the client or server the respective agent polls their folder and picks up that there is a change available. Then the options which were agreed upon during the connection stage are used to read the incomming message.

On failure of reading the message a error :q
 displayed.



## Limitations

- only a single message is able to be sent at this time
- manual setup of the folder permissions must be set up on each machine that this program is run on
- we are currently only supporting Ubuntu 17.10.
