# Demo DiSSCo Translator Service

## Description
The DiSSCo Translator Service is an application aimed at collecting and translating data and pushing it into the queue.
The collection currently supports three different methods:
###Naturalis Streaming API
Able to use the bioPortal streaming API from Naturalis `https://api.biodiversitydata.nl/v2/specimen/download` and download all specimen data.
This will be done completely streaming.

###GeoCase Json API
Able to request the GeoCase API. This will be done in request batches of X size (defined by `webclient.items-per-request`). Through pagination, it will walk through the complete GeoCase api and collect all specimen. It will publish each batch before retrieving the next.

###BioCase XML API
Able to request BioCase API's. This will be done in request batches of X size (defined by `webclient.items-per-request`). Through pagination, it will walk through the complete API and collect all the specimen. It will publish each batch before retrieving the next. 

###DWCA API
This will download the complete DWCA zip-file to local storage and unpack it before processing the complete file. It uses the GBIF library for row parsing. All records will be published when the complete file has been processed.

The application collects data from one of these data sources and translates it to an Open Digital Specimen (OpenDS) object.
During this translation it will try to collect the RoR identifier (success based on `chosen` field) if possible.
The RorService uses caching to prevent unnecessary calls.
It will collect the data as a stream and push this stream to Kafka message broker.
When all data has been collected the application will close itself.

### Kafka
For Kafka the application both creates the topic (`KafkaAdminConfig`) and produces to the newly created topic `KafkaProducerConfig`

## Parameter explanation
Parameters should be supplied as environmental arguments.
Application is expected to run as a docker container or kubernetes job.
Running als commandline application will require code changes (when providing the properties for the envs).

### Spring application
This property determines which service will be used by the application.  
`spring.profiles.active` The current options are `naturalis`, `geoCase`, `bioCase` (see `Profiles`)  

### Webclient parameters
`webclient.endpoint` The endpoint which will be requested for example `http://biocase.senckenberg.de//pywrapper.cgi`  
`webclient.query-params` Additional query parameters for example `?dsa=sgn_africanplants&request=` for bioCase request keep the request empty as it will be filled by the application.  
`webclient.items-per-request` Amount of items per request (for `geoCase` and `bioCase`) default is `1000`. With `bioCase` watch for limits such as for `http://biocase.senckenberg.de` this is 20 items per request.  
`webclient.content-namespace` For bioCase we need to specify the namespace `http://www.tdwg.org/schemas/abcd/2.06`  

### Kafka parameters
`kafka.host` The hostname (including port) for the Kafka server/cluster, for example `localhost:9092`  
`kafka.topic` The name of the topic  
`kafka.log-after-lines` The amount of objects after will a log line will be generated, default is `1000`  

### dwca parameters
`dwca.download-file` The location where the Darwin Core Archive zip-file will be saved to  
`dwca.temp-folder` The folder in which the unpacked Darwin Core Archive files will be saved to  

### Examples

#### BioCase
```
spring.profiles.active=bioCase
kafka.host=localhost:9092
kafka.topic=topic
webclient.endpoint=http://biocase.senckenberg.de//pywrapper.cgi
webclient.query-params=?dsa=sgn_africanplants&request=
webclient.items-per-request=20
webclient.content-namespace=http://www.tdwg.org/schemas/abcd/2.06
```
####GeoCase
```
spring.profiles.active=geoCase
kafka.host=localhost:9092
kafka.topic=topic
webclient.endpoint=https://api.geocase.eu/v1/solr
webclient.query-params=?q=*
```
####Naturalis
```
spring.profiles.active=naturalis
kafka.host=localhost:9092
kafka.topic=topic
webclient.endpoint=https://api.biodiversitydata.nl/v2/specimen/download
```

####Darwin Core Archive
```
spring.profiles.active=dwca
kafka.host=localhost:9092
kafka.topic=topic
webclient.endpoint=http://ipt.naturalsciences.be/ipt/archive.do?r=be_rbins_invertebrates_crustacea
dwca.download-file=src/main/resources/darwin.zip
dwca.temp-folder=src/main/resources/darwin/temp
```
### Installation instructions

### IDE
Pull the code from Github and open in your IDE.
Fill in the `application.properties` with the parameters described above.
Run the application.

### Maven 
The application is build with Maven and can be package as `jar` with `maven clean package`.

### Docker
Ensure that parameters are either available as environmental variables are added in the `application.properties`.
Build the Dockerfile with `docker build . -t translator-service`
Run the container with `docker run translator-service`

### Kubernetes
Added is a Kubernetes yaml for easy deployment on a kubernetes cluster.


## Known improvements
### Optimize the BioCase XML stream
Currently, the BioCase gathers all the data in separate (small calls).
This might be optimized so that it pushes the already collected data instead of first collecting everything.
Speed is an issue here however the BioCase response are very large which makes collected in larger batches difficult.
Object mapping might make a difference here, could be further explored.
