import sys
from setuptools import setup, find_packages
from shutil import copy,copytree,rmtree

if sys.version_info < (3,):
    sys.exit('Sorry, Python 2.x is not supported')

if sys.version_info > (3,) and sys.version_info < (3, 4):
    sys.exit('Sorry, Python3 version < 3.4 is not supported')

exec(open('src/main/python/dso/version.py').read())

rmtree("src/main/python/dso/java",ignore_errors=True)
copytree("target/lib","src/main/python/dso/java")
copy("target/dso-python-2.0.jar","src/main/python/dso/java")

setup(
    name='dso',
    version=__version__,
    url='https://github.com/crucial-project/dso',
    author='Pierre Sutra',
    description='Python bindings for the DSO datastore',
    long_description='Python bindings for the DSO datastore',
    author_email='pierre.sutra@telecom-sudparis.eu',
    package_dir={'':'src/main/python'},
    packages=find_packages('src/main/python'),
    install_requires=['JPype1'],
    include_package_data=True,
    package_data={
        'dso': ['java/*.jar']
    },
    python_requires='>=3.6',
    classifiers=[
        "Development Status :: 2 - Pre-Alpha",
        "Topic :: Utilities",
        "License :: OSI Approved :: Apache Software License"
    ],
)
