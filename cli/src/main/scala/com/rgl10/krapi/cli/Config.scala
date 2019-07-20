package com.rgl10.krapi.cli

import com.rgl10.krapi.common._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.scopt._
import scopt.OParser

final case class Config(url: String = "localhost:8080",
                        mode: Mode = Metadata,
                        entityType: MetadataType = Topics,
                        entityName: String = "",
                        keyDeserializer: SupportedType = SupportedType.String,
                        valueDeserializer: SupportedType = SupportedType.String,
                        prettyPrint: Boolean = false)

object Config {
  val parser: OParser[_, Config] = {
    val builder = OParser.builder[Config]

    import builder._
    OParser.sequence(
      programName("krapi-cli"),
      opt[String Refined Url]('u', "url")
        .required()
        .action((x, c) => c.copy(url = x.value))
        .valueName("")
        .text("url of the running krapi server e.g. http://localhost:8080"),
      opt[Mode]('m', "mode")
        .required()
        .action((x, c) => c.copy(mode = x))
        .valueName("")
        .text("either 'M'etadata or 'C'onsumer"),
      opt[MetadataType]("entityType")
        .action((x, c) => c.copy(entityType = x))
        .valueName("")
        .text("type of entity ('topics' (default) or 'consumergroups') to describe or list"),
      opt[String]("entityName")
        .action((x, c) => c.copy(entityName = x))
        .valueName("")
        .text("name of entity to describe or consume from"),
      opt[SupportedType]("keyDeserializer")
        .action((x, c) => c.copy(keyDeserializer = x))
        .valueName("")
        .text("deserializer for record keys - 'string' (default), 'long' or 'avro'"),
      opt[SupportedType]("valueDeserializer")
        .action((x, c) => c.copy(valueDeserializer = x))
        .valueName("")
        .text("deserializer for record values - 'string' (default), 'long' or 'avro'"),
      opt[Unit]("pretty")
        .action((_, c) => c.copy(prettyPrint = true))
        .valueName("")
        .text("pretty print output consumer stream"),
      help("help") text("prints this usage text")
    )
  }
}
