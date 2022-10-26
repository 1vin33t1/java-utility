# Java Utility

This micro-service is used for a common utility:
[delay-queue-worker]()

&nbsp;
## Technologies used:

* Java 11
* Spring-boot 2.4.5
* Spring Kafka 2.6.7
* Spring Aerospike data 2.4.2.RELEASE
* Aerospike 4.4.18
* Build Tool - Maven 3.6

&nbsp;
## Local Setup

* Clone this repository using the below command.

```shell script
git clone 
```

* Run the below command from the repository root location:

```shell script
mvn clean package
```

* To deploy **delay-queue-utility** project, use the output jar from the above command with vm arguments:

```jvm
    -Dspring.profiles.active=dev
    -Dspring.config.location=<location to the config folder locally, should be identical to the folder present here: utility-config/config/delay-queue-worker>
    -Dlog_path=<location to generate the logs>
```

* If you have a local **sonar** running, issue the below command for running sonar analysis:

```shell script
mvn verify sonar:sonar
```

&nbsp;
## Important links:

* [Jenkins delay-queue-utility](TODO://)
* [Kibana delay-queue-utility](TODO://)
* [Grafana delay-queue-utility](TODO://)

&nbsp;
## Contributing

Pull requests are welcome. For **major** changes, please open an issue first to discuss what you would like to change.
Please make sure to update tests as appropriate.

&nbsp;
## License

It's **FREE!** 😁
