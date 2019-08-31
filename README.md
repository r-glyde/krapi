# KRAPI

KRAPI is a **K**afka **R**EST **API** using [http4s](https://github.com/http4s/http4s) and [fs2-kafka](https://github.com/ovotech/fs2-kafka) providing read access to a kafka cluster.

### SETUP
`KAFKA_BROKERS` and `SCHEMA_REGISTRY_URL` should be provided as environment variables. `KRAPI_PORT` can also be provided or else the server will default to running on port 8080.

### Routes
#### GET
`/api/metadata/topics` => list topics

`/api/metadata/topics/TOPICNAME` => show configuration for topic `TOPICNAME`

`/api/metadata/consumergroups` => list consumer groups

`/api/metadata/consumergroups/GROUPID` => show consumer group metadata

#### POST
POST requests to the consumer routes will stream records from the relevant topic.

The response for these POST requests is a stream of gzipped JSON data.

Example payload:
```json
{
  "topic": "TOPICNAME",
  "keyDeserializer": "string|long|avro",
  "valueDeserializer": "string|long|avro"
}
```
`/api/consumer` => return all records for given `TOPICNAME`

`/api/consumer?key=0001` => return record(s) matching the given `key` 

### CLI
A CLI application is provided in the `krapi-cli` module.
```
$ krapi-cli --help
Usage: krapi-cli --url <string> --mode <string> [--entityType <string>] [--entityName <string>] [--keyDeserializer <string>] [--valueDeserializer <string>] [--pretty]

CLI tool to interact with a krapi server

Options and flags:
    --help
        Display this help text.
    --url <string>
        url of the running krapi server
    --mode <string>
        either 'M'etadata or 'C'onsumer
    --entityType <string>
        type of entity ('topics' or 'consumergroups') to describe or list
    --entityName <string>
        name of entity to describe or consume from
    --keyDeserializer <string>
        deserializer for record keys - 'string' (default), 'long' or 'avro'
    --valueDeserializer <string>
        deserializer for record values - 'string' (default), 'long' or 'avro'
    --pretty
        pretty print output consumer stream

```
