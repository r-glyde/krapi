# KRAPI

KRAPI is a **K**afka **R**EST **API** using [http4s](https://github.com/http4s/http4s) and [fs2-kafka](https://github.com/ovotech/fs2-kafka) providing read access to a kafka cluster.

### SETUP
`KAFKA_BROKERS` and `SCHEMA_REGISTRY_URL` should be provided as environment variables. `KRAPI_PORT` can also be provided or else the server will default to running on port 8080.

### Routes
#### GET
`/api/metadata/topics` => list topics

`/api/metadata/topics/TOPICNAME` => list configuration for topic `TOPICNAME`

`/api/metadata/groups` => not implemented

`/api/metadata/groups/GROUPNAME` => not implemented

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
Usage: krapi-cli [options]

  -u, --url             url of the running krapi server e.g. http://localhost:8080
  -m, --mode            either 'M'etadata or 'C'onsumer
  --entityType          type of entity ('topics' (default) or 'consumer-groups') to describe or list
  --entityName          name of entity to describe or consume from
  --keyDeserializer     deserializer for record keys - 'string' (default), 'long' or 'avro'
  --valueDeserializer   deserializer for record values - 'string' (default), 'long' or 'avro'
  --pretty              pretty print output consumer stream
  --help                prints this usage text
```
