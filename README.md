# Demo Webflux Naturalis

## Description
Naturalis creates an API for streaming download of the data.
This application sets up a stream with the Naturalis API, maps the data and pushes it to Kafka.
While mapping the application request RoR for the RoR identifier for the institution (uses a cache to prevent unnecessary calls).
When all the specimen are pulled from the Naturalis API, the application will quit (exit code 0).

### Kafka
For Kafka the application both creates the topic (`KafkaAdminConfig`) and produces to the newly created topic `KafkaProducerConfig`

## Parameter explanation
Parameters should be supplied as environmental arguments.
Application is expected to run as a docker container or kubernetes job.
Running als commandline application will require code changes (when providing the properties for the envs).

### Kafka parameters

`kafka.host` The hostname (including port) for the Kafka server/cluster, for example `localhost:9092`
`kafka.topic` The name of the topic 

### Installation instructions

### IDE
Pull the code from Github and open in your IDE.
Fill in the `application.properties` with the parameters described above.
Run the application.

### Maven 
The application is build with Maven and can be package as `jar` with `maven clean package`.

### Docker
Ensure that parameters are either available as environmental variables are added in the `application.properties`.
Build the Dockerfile with `docker build . -t demo-naturalis-api`
Run the container with `docker run demo-naturalis-api`

### Kubernetes
Added is a Kubernetes yaml for easy deployment on a kubernetes cluster.
