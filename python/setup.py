import sys
from setuptools import setup, find_packages
from shutil import copy,copytree,rmtree

if sys.version_info < (3,):
    sys.exit('Sorry, Python 2.x is not supported')

if sys.version_info > (3,) and sys.version_info < (3, 4):
    sys.exit('Sorry, Python3 version < 3.4 is not supported')

exec(open('src/main/python/creson/version.py').read())

rmtree("src/main/python/creson/java",ignore_errors=True)
copytree("target/lib","src/main/python/creson/java")
copy("target/infinispan-creson-python-9.4.16.Final.jar","src/main/python/creson/java")

setup(
    name='creson',
    version=__version__,
    url='https://github.com/creson',
    author='Pierre Sutra',
    description='Python bindings for the Creson framework',
    long_description="Python bindings for the Creson framework",
    author_email='pierre.sutra@telecom-sudparis.eu',
    package_dir={'':'src/main/python'},
    packages=find_packages('src/main/python'),
    install_requires=['JPype1'],
    include_package_data=True,
    package_data={
        'creson': ['java/*.jar']
    },
    python_requires='>=3.6',
    classifiers=[
        "Development Status :: 2 - Pre-Alpha",
        "Topic :: Utilities",
        "License :: OSI Approved :: Apache Software License"
    ],
)
