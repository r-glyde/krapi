package com.rgl10.krapi.cli

import cats.data.Validated.{invalidNel, valid}
import cats.data.ValidatedNel
import com.monovore.decline._
import com.monovore.decline.refined._
import com.rgl10.krapi.common._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

object Config {

  val cliCommand: Opts[CliApp] => Command[CliApp] =
    Command[CliApp](name = "krapi-cli", header = "CLI tool to interact with a krapi server")

  implicit val readDeserializer: Argument[SupportedType] = new Argument[SupportedType] {
    override def read(input: String): ValidatedNel[String, SupportedType] = input.toLowerCase match {
      case "string" => valid(SupportedType.String)
      case "long"   => valid(SupportedType.Long)
      case "avro"   => valid(SupportedType.Avro)
      case _        => invalidNel(s"Invalid deserialiser, '$input': should be 'string', 'long' or 'avro'")
    }
    override def defaultMetavar: String = "string"
  }

  val urlOpt =
    Opts.option[String Refined Url]("url", help = "url of the running krapi server")
  val modeOpt =
    Opts
      .option[String]("mode", help = "either 'M'etadata or 'C'onsumer", metavar = "string")
      .mapValidated(_.toUpperCase match {
        case "M"   => valid(Mode.Metadata)
        case "C"   => valid(Mode.Consumer)
        case other => invalidNel(s"Invalid mode, '$other': should be either 'M' or 'C'")
      })
  val entityTypeOpt =
    Opts
      .option[String]("entityType", "type of entity ('topics' or 'consumergroups') to describe or list")
      .mapValidated(_.toLowerCase match {
        case "topics"         => valid(MetadataType.Topics)
        case "consumergroups" => valid(MetadataType.ConsumerGroups)
        case other            => invalidNel(s"Invalid entity type, '$other': should be either 'topics' or 'consumergroups'")
      })
      .orNone

  val entityNameOpt =
    Opts.option[String]("entityName", "name of entity to describe or consume from").orNone
  val keyDeserializerOpt =
    Opts
      .option[SupportedType]("keyDeserializer", "deserializer for record keys - 'string' (default), 'long' or 'avro'")
      .withDefault(SupportedType.String)
  val valueDeserializerOpt =
    Opts
      .option[SupportedType](
        "valueDeserializer",
        "deserializer for record values - 'string' (default), 'long' or 'avro'"
      )
      .withDefault(SupportedType.String)
  val prettyPrintOpt =
    Opts.flag("pretty", "pretty print output consumer stream").orFalse

}
