import os

import jpype
from jpype import *

if "CLASSPATH" in os.environ:
    classpath = os.environ["CLASSPATH"]
    jpype.addClassPath(classpath)

startJVM(getDefaultJVMPath(), "-ea", convertStrings=False)
testPkg = JPackage("org.infinispan.creson")
server=testPkg.PythonFactory.DEFAULT_SERVER

if "SERVER" in os.environ:
    server = os.environ["SERVER"]

f = testPkg.PythonFactory(server)

c = f.createCounter("cnt")

print(c.increment())

shutdownJVM()
