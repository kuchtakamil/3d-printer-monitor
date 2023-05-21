# 3D Printer Monitor

3D Printer Monitor is a set of three Scala applications which simulates the behaviour of some IoT sensors which read 
data describing some properties of working 3D Printer.

We have two `sensor-simulator`s:
    
- bed temperature
- carriage speed

and also a `data consumer` which pools data from the Kafka, classifies them putting label `valid` or `invalid` based on 
data in configuration file, exposes a websocket server which pushes mentioned data through the websocket.


Concept diagram for 
![3d-printer-v1.drawio.png](img%2F3d-printer-v1.drawio.png)

### How to run 

    docker-compose up
After running this command, several docker containers will be created including Kafka, two sensor simulators and a consumer.

The consumer exposes websocket server, which sends the data which can be visualized with use of [3D Printer Monitor Web App] 
localized on the another GitHub repository. 


[3D Printer Monitor Web App]: <https://github.com/kuchtakamil/3d-printer-monitor-web>