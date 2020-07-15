#!/bin/sh

cp ./out/artifacts/WebRequestProcessor_jar/WebRequestProcessor.jar ./WebRequestProcessor.jar

JAVA_HOME="/usr/lib/jvm/jdk1.8.0_144"

$JAVA_HOME/bin/java -jar ./WebRequestProcessor.jar showui