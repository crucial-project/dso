import os
import sys
import jpype
from jpype import *

class Factory:

    def __init__(self,server="127.0.0.1:11222"):

        dir = os.path.dirname(os.path.abspath(__file__))
        classpath=dir+"/java/*"
        jpype.addClassPath(classpath)
        startJVM(getDefaultJVMPath(), "-ea", convertStrings=False)

        self.Package = JPackage("org.infinispan.creson")
        self.factory = self.Package.PythonFactory(server)

    def createCounter(self,name,value=0):
        return self.factory.createCounter(name,value)

    def createMap(self,name):
        return self.factory.createMap(name)

