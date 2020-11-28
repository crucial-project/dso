import os
import sys
import jpype
from jpype import *

class Client:

    def __init__(self,server="127.0.0.1:11222"):

        dir = os.path.dirname(os.path.abspath(__file__))
        classpath=dir+"/java/*"
        jpype.addClassPath(classpath)
        startJVM(getDefaultJVMPath(), "-ea", convertStrings=False)

        self.Package = JPackage("org.crucial.dso")
        self.client = self.Package.client.Client.getClient(server)

    def getLog(self, name) :
        return self.client.getLog(name)

    def getAtomicList(self,  name):
        return self.client.getAtomicList(name)

    def getFuture(self, name, new):
        return self.client.getFuture(name, new)

    def getMonitorCyclicBarrier(self, name, parties):
        return self.client.getMonitorCyclicBarrier(name, parties)

    def getCyclicBarrier(self, name, parties):
        return self.client.getCyclicBarrier(name, parties)

    def getScalableCyclicBarrier(self, name, parties):
        return self.client.getScalableCyclicBarrier(name, parties)

    def getSemaphore(self, name, permits=0):
        return self.client.getSemaphore(name, permits)

    def getAtomicInt(self, name, value=0):
        return self.client.getAtomicInt(name, value)

    def getAtomicLong(self, name):
        return self.client.getAtomicLong(name)

    def getAtomicByteArray(self, name):
        return self.client.getAtomicByteArray(name)

    def getAtomicBoolean(self, name, value=0):
        return self.client.getAtomicBoolean(name, value)

    def getAtomicCounter(self, name, value=0):
        return self.client.getAtomicCounter(name, value)

    def getMap(self, name):
        return self.client.getMap()

    def getAtomicMap(self, name):
        return self.client.getAtomicMap(name)

    def getAtomicMatrix(self, name, type="java.lang.Integer", n=0, m=0):
        clazz = JClass(type)
        return self.client.getAtomicMatrix(name, clazz, n, m)

    def getBlob(self, name):
        return self.client.getBlob(name)

    def getCountDownLatch(self, name, parties):
        return self.client.getCountDownLatch(name, parties)

    def clear(self):
        self.client.clear()

    def close(self):
        self.client.close()
