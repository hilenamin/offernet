#!/bin/bash

# launch Zipkin in docker container 
# docker run -d -p 9411:9411 openzipkin/zipkin

# launch Jaeger in docker container
docker run -d -e \
  COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 9411:9411 \
  jaegertracing/all-in-one:latest

# launch Prometheus in docker container 
docker run -p 9090:9090 -v $PWD/analysis/configs/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus