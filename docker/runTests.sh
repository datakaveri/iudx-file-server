#!/bin/bash

nohup mvn clean compile exec:java@file-server & 
sleep 20
mvn clean test
cp -r target /tmp/test/
