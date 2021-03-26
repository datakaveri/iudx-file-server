#!/bin/bash

# To be executed from project root
docker build -t iudx/fs-depl:latest -f docker/depl.dockerfile .
docker build -t iudx/fs-dev:latest -f docker/dev.dockerfile .
