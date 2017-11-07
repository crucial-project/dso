#  The Creson framework

Creson is a general purpose synchronization framework.
It allows to share, persist and call Java objects remotely.
The current implementation is based on [Infinispan](http://infinispan.org/).

## Introduction

Cloud applications are commonly split in three distinct tiers, the presentation tier, the application tier and the storage tier.
The presentation tier displays information related to the application services, such as web pages.
The application tier contains the business logic.
The storage tier executes the data persistence mechanisms (database servers, file shares, etc.) and the data access layer that encapsulates the persistence mechanisms and exposes the data.

Traditionally, an [object-relational mapping](https://en.wikipedia.org/wiki/Object-relational_mapping
) (ORM) [converts the data between the application and the storage tier.
The ORM materializes the frontier between the two tiers, and it reduces the coupling. 
However, it also forces to repeatedly convert the objects between their in-memory and their serialized representations.
This negatively impacts performance and increases execution costs.

Creson is a general purpose synchronization framework.
With Creson, instead of fetching objects from the storage, the application directly calls them.
Creson ensures that the objects are persisted and shared consistently among several client machines.

## Programming with Creson 

To declare a Creson object, the programmer uses the keyword `@Shared` on the field of an object.
As an example, consider the following two classes.

	class Hero{@Shared Room location;}
	class Room{Treasure loot();}

The `Hero` class contains a `location` field annotated with `@shared`.
This tells Creson to push the `location` to the storage tier, allowing several instances of `Hero` on several application machines to access the same `location` object transparently.

Creson ensures that the object are _strongly consistent_ over time.
In the example above, this means that if two heroes stand in the same rooom, only one of them may loot the treasure.
More precisely, the synchronization contract of every Creson object `o` is that `o` is atomic, aka. [linearizable](https://en.wikipedia.org/wiki/Linearizability).
In Java, this means that for every method `m`, `m` is called as `synchronized(o){o.m}`.

## Usage

Creson relies on a client-server architecture.
The current server implementation is based on [Infinispan](http://infinispan.org/).
To build an archive containing the server, use the `mvn package` at the root of the project.
The resulting archive, named `infinispan-creson-server-*.tar.gz`, is located in `server/target`.
Then, to launch the server run the script `server.sh` from the root of the archive.

Every class used by a client, e.g., the `Hero` class above, should be known at the server.
This requires to add the appropriates `.class` files or jars to the classpath of the server.
Alternatively, the server can dynamically load new jars (by default, they should be located in `/tmp`).

Some examples of Creson in conjunction with AWS Lambda are available in the [slambda](https://github.com/otrack/slambda) project.
To run them, clone the project, build a jar using `mvn package` then add it to `/tmp`.   

## How to deploy Creson in Amazon EC2 ?

The server shell script can be used with AWS EC2.
Internally, Creson uses Infinispan which itself relies on the [JGroups](http://www.jgroups.org/) stack for discovery and communication.
To deploy one or more servers in EC2, Creson uses the S3 ping facility of JGroups.

The configuration file for the server is `jgroups-creson-ec2.xml`.
To deploy the server in your own EC2 instances, you need to fix the following 3 parameters in this XML file. 

<S3_PING   
    location="your_bucket"  
    access_key="your_key"  
    secret_access_key="your_secret"  
    />

The access key and the corresponding secret are credentials to write in the bucket.
We advice you to [create](http://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html) an IAM in EC2 for that purpose.

## Advanced usage

Methods annotated with `@ReadOnly` allow to execute the corresponding operation at the client tier.
This is done by fetching the full state on the client, similarly to a common ORM.

A call to a `@ReadOnly` method is consistent, yet ot does not necessarily see latest state of the object.
In other words, for the programmer the object is now [sequentially consistent](https://en.wikipedia.org/wiki/Sequential_consistency).

## White paper

A white paper about Creson was published in the proceedings of the 37th IEEE International Conference on Distributed Computing Systems (ICDCS 2017). 
A preprint is available [here](https://drive.google.com/open?id=0BwFkGepvBDQoR3FNQk9VY1U2Q1U)
