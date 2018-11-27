## Requirements
- Install java:
  - [Ubuntu java installation instruction](#install-java-for-ubuntu).
  - For Windows download Java [here](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html) and install.
- Install the latest [maven build tool](https://maven.apache.org/download.cgi);

## How to build
Blynk has a bunch of integration tests that require DB, so you have to skip tests during build.
- Build the project with : ```mvn clean install -Dmaven.test.skip=true```

## Turn off chrome https warning on localhost

Local server uses by default self-generated certificates, so you have to disable tls/ssl on the localhost for your browser:

- Paste in chrome

        chrome://flags/#allow-insecure-localhost

- You should see highlighted text saying: "Allow invalid certificates for resources loaded from localhost". Click enable.


## Installing DB

#### 1. a) Install PostgreSQL.

        sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
        wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O - | sudo apt-key add -
        
        sudo apt-get update
        sudo apt-get install postgresql postgresql-contrib
        
#### 1. b) Install PostgreSQL.

        sudo apt-get update
        apt-get --no-install-recommends install postgresql-9.6 postgresql-contrib-9.6

#### 2. Download Blynk DB script

        wget https://raw.githubusercontent.com/blynkkk/dash/master/server/core/src/main/resources/create_schema.sql?token=ABdx7pmBJ_DKVb5EMoU2DqMAq3LuBg7Aks5b8oPdwA%3D%3D
        wget https://raw.githubusercontent.com/blynkkk/dash/master/server/core/src/main/resources/reporting_schema.sql?token=ABdx7k1gDnSZ5viAlvFJDzmNcBq396AQks5b8oPHwA%3D%3D

#### 3. Move create_schema.sql and reporting_schema.sql to temp folder (to avoid permission problems)

        mv create_schema.sql /tmp
        mv reporting_schema.sql /tmp
        
Result:  

        /tmp/create_schema.sql
        /tmp/reporting_schema.sql
        
Copy this paths to clipboard from your console.

#### 5. Connect to PostgreSQL

        sudo su - postgres
        psql

#### 6. Create Blynk DB and Reporting DB, test user and tables

        \i /tmp/create_schema.sql
        \i /tmp/reporting_schema.sql
        
```/tmp/create_schema.sql``` - is path from step 3.
        
You should see next output:

        postgres=# \i /tmp/create_schema.sql
        CREATE DATABASE
        You are now connected to database "blynk" as user "postgres".
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE TABLE
        CREATE ROLE
        GRANT
        GRANT

#### Quit

        \q
               
Now start your server and you should see next text in ```postgres.log``` file : 

        2017-03-02 16:17:18.367 - DB url : jdbc:postgresql://localhost:5432/blynk?tcpKeepAlive=true&socketTimeout=150
        2017-03-02 16:17:18.367 - DB user : test
        2017-03-02 16:17:18.367 - Connecting to DB...
        2017-03-02 16:17:18.455 - Connected to database successfully.


### Automatic Let's Encrypt certificates generation

Latest Blynk server has super cool feature - automatic Let's Encrypt certificates generation. 
However, it has few requirements: 
 
+ Add ```server.host``` property in ```server.properties``` file. 
For example : 
 
        server.host=myhost.com

IP is not supported, this is the limitation of Let's Encrypt. Also have in mind that ```myhost.com``` 
should be resolved by public DNS severs.
        
+ Add ```contact.email``` property in ```server.properties```. For example : 
 
        contact.email=test@gmail.com
        
+ You need to start server on port 80 (requires root or admin rights) or 
make [port forwarding](#port-forwarding-for-https-api) to default Blynk HTTP port - 8080.

That's it! Run server as regular and certificates will be generated automatically.

![](https://gifyu.com/images/certs.gif)

### Enabling mail on Local server
To enable mail notifications on Local server you need to provide your own mail credentials. Create file `mail.properties` within same folder where `server.jar` is.
Mail properties:

        mail.smtp.auth=true
        mail.smtp.starttls.enable=true
        mail.smtp.host=smtp.gmail.com
        mail.smtp.port=587
        mail.smtp.username=YOUR_EMAIL_HERE
        mail.smtp.password=YOUR_EMAIL_PASS_HERE

Find example [here](https://github.com/blynkkk/blynk-server/blob/master/server/notifications/email/src/main/resources/mail.properties).

WARNING : only gmail accounts are allowed.

NOTE : you'll need to setup Gmail to allow less secured applications.
Go [here](https://www.google.com/settings/security/lesssecureapps) and then click "Allow less secure apps".

### Install java for Ubuntu

        sudo add-apt-repository ppa:linuxuprising/java
        sudo apt-get update
        sudo apt-get install oracle-java10-installer
        
or if above doesn't work:

        sudo apt-add-repository ppa:webupd8team/java
        sudo apt-get update
        sudo apt-get install oracle-java8-installer
        
### Port forwarding for HTTP/S API

        sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
        sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 9443

        
### How Blynk Works?
When hardware connects to Blynk cloud it opens either keep-alive ssl/tls connection on port 443 (9443 for local servers) or keep-alive plain
tcp/ip connection on port 8080. Blynk app opens mutual ssl/tls connection to Blynk Cloud on port 443 (9443 for local servers).
Blynk Cloud is responsible for forwarding messages between hardware and app. In both (app and hardware) connections Blynk uses 
own binary protocol described below.

### Blynk protocol


#### Hardware side protocol

Blynk transfers binary messages between the server and the hardware with the following structure:

| Command       | Message Id    | Length/Status   | Body     |
|:-------------:|:-------------:|:---------------:|:--------:|
| 1 byte        | 2 bytes       | 2 bytes         | Variable |

Command and Status definitions: [BlynkProtocolDefs.h](https://github.com/blynkkk/blynk-library/blob/7e942d661bc54ded310bf5d00edee737d0ca44d7/src/Blynk/BlynkProtocolDefs.h)


#### Mobile app side protocol

Blynk transfers binary messages between the server and mobile app with the following structure:

| Command       | Message Id    | Length/Status   | Body     |
|:-------------:|:-------------:|:---------------:|:--------:|
| 1 byte        | 2 bytes       | 4 bytes         | Variable |


#### Websockets web side protocol

Blynk transfers binary messages between the server and websockets (for web) with the following structure:

| Websocket header   | Command       | Message Id    | Body     |
|:------------------:|:-------------:|:-------------:|:--------:|
|                    | 1 byte        | 2 bytes       | Variable |


When command code == 0, than message structure is next:

| Websocket header   | Command       | Message Id    | Response code |
|:------------------:|:-------------:|:-------------:|:-------------:|
|                    | 1 byte        | 2 bytes       | 4 bytes       |

[Possible response codes](https://github.com/blynkkk/blynk-server/blob/master/server/core/src/main/java/cc/blynk/server/core/protocol/enums/Response.java#L12).
[Possible command codes](https://github.com/blynkkk/blynk-server/blob/master/server/core/src/main/java/cc/blynk/server/core/protocol/enums/Command.java#L12)

Message Id and Length are [big endian](http://en.wikipedia.org/wiki/Endianness#Big-endian).
Body has a command-specific format.

## Licensing
[GNU GPL license](https://github.com/blynkkk/blynk-server/blob/master/license.txt)
