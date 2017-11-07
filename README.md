# securedchat
Toy project to do do secure local messaging.

The server and client run in different processes. They need to be run in different
command line terminals. They use AES encryption to perform authentication although
this is easliy changed;

# Sever

Compile the client/server in the src folder

```
javac SecureChat.java
```

then run the server with 

`java SecureChat --server`

then run the client (in a separate terminal)

`java SecureChat --client`


