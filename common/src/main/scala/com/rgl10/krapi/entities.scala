package com.rgl10.krapi

final case class Topic(name: String, partitions: Int, replicationFactor: Int)
final case class ConfigItem(name: String, value: String, isDefault: Boolean, isReadOnly: Boolean)
final case class Configuration(topic: Topic, config: List[ConfigItem])
final case class Record[K, V](topic: String, key: Option[K], value: Option[V], partition: Int, timestamp: Long)
final case class SubscriptionDetails(topic: String, keyDeserializer: String, valueDeserializer: String)
