#!/bin/sh

scp ../server/launcher/target/airiusfans-0.39.13-SNAPSHOT.jar root@airiusfans.blynk.cc:/root
scp ../server/core/src/main/resources/create_schema.sql root@104.131.50.204:/tmp