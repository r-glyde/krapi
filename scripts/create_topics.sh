#!/usr/bin/env bash
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-1 --partitions 1 --replication-factor 1 --config cleanup.policy=compact
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-2 --partitions 1 --replication-factor 1
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-3 --partitions 3 --replication-factor 1
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-4 --partitions 5 --replication-factor 1
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-5 --partitions 5 --replication-factor 1
kafka-topics.sh --zookeeper localhost:2181 --create --topic topic-6 --partitions 5 --replication-factor 1

for i in {1..100}; do
    echo "$i:message$i" | kafkacat -P -b localhost:9092 -t topic-3 -K:
done
