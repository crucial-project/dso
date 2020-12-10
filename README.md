#  DSO

The distributed shared objects (DSO) datastore allows to share, call and persist objects remotely.

## For the impatient

	git clone https://github.com/crucial-project/dso
	./dso/client/src/test/bin/local/test.sh -create // create a server using docker
	./dso/client/src/test/bin/local/test.sh -[blobs|counters|countdownlatch|barrier|sbarrier]
	./dso/client/src/test/bin/local/test.sh -delete // delete the server

## Introduction

Cloud applications are commonly split into three distinct tiers: presentation, application and storage.
The presentation tier displays information related to the application services, such as web pages.
The application tier contains the business logic.
The storage tier is in charge of serving and persisting data.

Traditionally, an [object-relational mapping](https://en.wikipedia.org/wiki/Object-relational_mapping) (ORM) converts the data between the application and storage tiers.
The ORM materializes the frontier between the two tiers, and it reduces the coupling. 
However, it also forces to repeatedly convert the objects between their in-memory and their serialized representations back and forth.
This negatively impacts performance and increases execution costs.

DSO is a general-purpose synchronization and data sharing framework.
With DSO, instead of fetching objects from storage, the application directly calls them.
DSO ensures that the objects are persisted and shared consistently among several client machines.

## Programming with DSO 

DSO offers several client-side programming libraries.
The most complete one is for the Java language.
To declare a DSO object in Java, the programmer uses the keyword `@Shared` on the field of an object.
As an example, consider the following two classes.

	class Hero{@Shared Room location;}
	class Room{Treasure loot();}

The `Hero` class contains a `location` field annotated with `@shared`.
This tells DSO to push the `location` to the storage tier, allowing several instances of `Hero` on several application machines to access the same `location` object transparently.

DSO ensures that the object are _strongly consistent_ over time.
In the example above, this means for instance that if two heroes stand in the same room, only one of them may loot the treasure.
More precisely, the synchronization contract of every DSO object `o` is that `o` is atomic, aka. [linearizable](https://en.wikipedia.org/wiki/Linearizability).
In Java, this is equivalent to guarding every method `m` of some object `o` with `synchronized(o){o.m}`.

DSO includes a library of shared objects (counter, integer, list, maps, barrier, etc.).
The provided objects are listed in the [client](https://github.com/crucial-project/dso/tree/master/client/src/main/java/org/crucial/dso) module.

DSO follows a standard client-server architecture.
The current server implementation is layered above [Infinispan](http://infinispan.org/).

The project includes libraries for [Python](https://github.com/crucial-project/dso/tree/master/python) as well as the [Shell](https://github.com/crucial-project/serverless-shell) language.

## Usage

Below, we explain how to deploy a DSO server and access it in different contexts.

### Docker

To run the server locally, one may type:

	docker run --net host --rm --env EXTRA="-rf 2" --env CLOUD=local --env PORT=11222 0track/dso-server:latest

Local (unit) tests are available under `./dso/client/src/test/bin/local/test.sh`.

### Kubernetes

To use DSO in a distributed context, the simplest approach is to rely on a container orchestrator.
The project includes a set of scripts for [Kubernetes](https://www.kubernetes.org) (k8s).
To run the tests in a k8s cluster, one may use the following commands:

	./client/src/test/bin/k8s/[aws,gcp]/bootstrap.sh
	./client/src/test/bin/k8s/test.sh -create
	./client/src/test/bin/k8s/test.sh -[blobs|counters|countdownlatch|barrier|sbarrier]
	./client/src/test/bin/k8s/test.sh -delete

### AWS EC2

As pointed above DSO uses internally Infinispan which itself relies on the [JGroups](http://www.jgroups.org/) stack for discovery and communication.
To deploy one or more servers in EC2, DSO uses the S3 ping facility of JGroups.

The configuration file for the server is `jgroups-dso-ec2.xml`.
To deploy the server in your own EC2 instances, you need to fix the following 3 parameters in this XML file. 

<S3_PING   
    location="your_bucket"  
    access_key="your_key"  
    secret_access_key="your_secret"  
    />

The access key and the corresponding secret are credentials to write in the bucket.
We advice you to [create](http://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html) an IAM in EC2 for that purpose.

### Hand-made deployment

It is also possible to build your own server, e.g., to deploy a specific library of shared objects.
To build an archive containing the server, use the `mvn install -DskipTests` at the root of the project.
The server archive, named `dso-server-*.tar.gz`, is located in `server/target`.
To launch the server run the script `server.sh` from the root of the archive.

Every class used by a client, e.g., the `Hero` class above, should be known at the server.
This requires to add the appropriates `.class` or `jar` files to the classpath of the server.
Alternatively, the server can dynamically load new jars (by default, they should be located in `/tmp`).

## Advanced examples

More uses cases are provided in the [examples](https://github.com/crucial-project/examples) repository of the Crucial project.

The [serverless shell](https://github.com/crucial-project/serverless-shell) uses DSO to synchronize distributed shell instances executed atop a FaaS infrastructure (e.g., AWS Lambda).

We used DSO to port the Random Forest classification algorithm in the [Smile](https://github.com/crucial-project/smile) library.

