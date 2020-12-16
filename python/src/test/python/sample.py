from dso.client import Client
from jpype import *
from jpype import java
import os

client = Client(os.environ.get('DSO'))

c = client.getAtomicCounter("cnt")
print("counter: "+str(c.increment()))

m1 = client.getAtomicMap("map")
m1[1]=1

try :
    print("merge: " + str(m1.merge(1, 3, client.Package.Sum())))
except java.lang.Throwable as ex:
    print(ex.stacktrace())

m2 = java.util.HashMap()
m2[3]=5

try :
    m1.mergeAll(m2, client.Package.Sum())
    print("mergeAll: "+str(m1.keySet()))
except java.lang.Throwable as ex:
    print(ex.stacktrace())

b = client.getCyclicBarrier("b", 1)
b.waiting()

l = client.getAtomicList("list")
l.append("hello")
l[0] = "test"
l[0] += "test2"
print(l.toArray())
