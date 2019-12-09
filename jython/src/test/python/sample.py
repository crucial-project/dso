import os

from org.infinispan.creson import JythonFactory

server=JythonFactory.DEFAULT_SERVER

if "SERVER" in os.environ:
    server = os.environ["SERVER"]

f = JythonFactory(server)

c = f.createCounter("cnt")

print c.increment()
