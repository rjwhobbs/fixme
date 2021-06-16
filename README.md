# fixme
A simulated electronic trading experiment implementing a basic FIX trading protocol consisting of three independent  
components that comunicate over a network namely the broker, market, and router.  
Focus is placed on multi-threaded network applications with asynchronous sockets and the java executor framework.

## Compilation
Ensure that the maven build tool is installed https://maven.apache.org/install.html  
Run `mvn clean package` in the root directory of the project.  

## Run
The project consists of three different progamms, the router, the market and the broker, each needs to be run seperately.  
To start the router `cd fixme-router/target` then `java -jar fixmerouter.jar`  
To start the market `cd fixme-market/target` then `java -jar fixmemarket.jar`  
To start the broker `cd fixme-broker/target` then `java -jar fixmebroker.jar`
