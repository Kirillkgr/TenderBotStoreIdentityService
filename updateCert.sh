#!/bin/bash
ls -a;
echo "Remove old cert"
rm -rf ssl;
ls -a;
echo "create contaner and make cert"
docker-compose -f getNewSll.yml upж
cd ssl;
echo "cert create"
ls -a;



