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

        self.Package = JPackage("org.crucial.dso")
        self.factory = self.Package.PythonFactory(server)

    def createCounter(self, name, value=0):
        return self.factory.createCounter(name,value)

    def createMap(self, name):
        return self.factory.createMap(name)

    def createMatrix(self, name, type="java.lang.Integer", n=0, m=0):
        clazz = JClass(type)
        return self.factory.createMatrix(name, clazz, n, m)

