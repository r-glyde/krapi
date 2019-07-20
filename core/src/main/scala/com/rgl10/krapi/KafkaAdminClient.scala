package com.rgl10.krapi

import cats.syntax.option._
import com.rgl10.krapi.common._
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.config.ConfigResource

import scala.collection.JavaConverters._

object KafkaAdminClient {
  implicit class AdminClientOps(val ac: AdminClient) extends AnyVal {
    def listTopics: Set[String] = ac.listTopics().names().get().asScala.toSet

    def getTopics: List[Topic] = {
      ac.describeTopics(listTopics.asJavaCollection).values().asScala.values.map { fd =>
        val desc  = fd.get()
        val parts = desc.partitions().asScala.toList
        Topic(desc.name(), parts.size, parts.head.replicas().size())
      }
    }.toList

    def getTopicConfig(topicName: String): Option[Configuration] =
      getTopics.find(_.name == topicName).fold(Option.empty[Configuration]) { topic =>
        val configEntries = ac
          .describeConfigs(Set(new ConfigResource(ConfigResource.Type.TOPIC, topicName)).asJavaCollection)
          .values()
          .asScala
          .values
          .flatMap(_.get().entries().asScala)
          .toList
        Configuration(topic, configEntries.map(c => ConfigItem(c.name(), c.value(), c.isDefault, c.isReadOnly))).some
      }

    def getConsumerGroups: Set[String] = ac.listConsumerGroups().all().get().asScala.map(_.groupId()).toSet

    def describeConsumerGroup(groupId: String): Option[ConsumerGroup] =
      if (getConsumerGroups.contains(groupId)) {
        val offsets = ac.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get().asScala.toMap.map {
          case (topicPartition, offset) => s"${topicPartition.topic()}-${topicPartition.partition()}" -> offset.offset()
        }
        ConsumerGroup(groupId, offsets).some
      } else None
  }
}
