from dso.factory import Factory
from jpype import *
from jpype import java

factory = Factory("35.245.240.1:11222")

c = factory.createCounter("cnt")
c.reset()
print("counter: "+str(c.increment()))

m1 = factory.createMap("map1")
m1.clear()
m1[1]=1

try :
    print("merge: "+str(m1.merge(1,3,factory.Package.Sum())))
except java.lang.Throwable as ex:
    print(ex.stacktrace())

m2 = java.util.HashMap()
m2[3]=5

try :
    m1.mergeAll(m2,factory.Package.Sum())
    print("mergeAll: "+str(m1.keySet()))
except java.lang.Throwable as ex:
    print(ex.stacktrace())
