import requests
import json
import argparse

def main():
  parser = argparse.ArgumentParser()
  general_opts = parser.add_argument_group('General')
  metadata_opts = parser.add_argument_group('Metadata')
  consumer_opts = parser.add_argument_group('Consumer')

  general_opts.add_argument('--url', help='Base URL of API e.g. \'hostname:port\' (omit protocol)', metavar='')
  general_opts.add_argument('--mode', help='(M)etadata or (C)onsumer', metavar='')
  general_opts.add_argument('--name', help='Entity to list, describe or consume from', metavar='')
  general_opts.add_argument('--pretty', help='Output pretty(ish) JSON', action='store_true')
  metadata_opts.add_argument('--type', help='One of \'topics\' or \'groups\'', metavar='')
  consumer_opts.add_argument('--keyDeserializer', help='One of \'string\' (default), \'long\' or \'avro\'', metavar='', default='string')
  consumer_opts.add_argument('--valDeserializer', help='One of \'string\' (default), \'long\' or \'avro\'', metavar='', default='string')
  consumer_opts.add_argument('--key', help='Query to filter by record key', metavar='')
  consumer_opts.add_argument('--contains', help='Query to filter records', metavar='')
  args = parser.parse_args()

  BASE_URL = "http://" + args.url + "/api"

  ### CONSUMER MODE
  if args.mode == "C" and args.name:
    url = BASE_URL + "/consumer"
    params = {}
    data = { "topic": args.name, "keyDeserializer": args.keyDeserializer, "valueDeserializer": args.valDeserializer }
    if args.key:
      params = { "key": args.key }
    if args.contains:
      params = { "contains": args.contains }
    print("[")
    for raw_record in stream_records(url, data, params):
      try:
          pretty_print(json.loads(raw_record), pretty = args.pretty, to_array = True)
      except ValueError:
          continue
    print("]")
  ### METADATA MODE
  elif args.mode == "M":
    url = BASE_URL + "/metadata"
    ### TOPICS
    if args.type == "topics":
      url += "/topics/"
      if args.name:
        response = requests.get(url + args.name)
      else:
        response = requests.get(url)
    ### CONSUMER-GROUPS
    elif args.type == "groups":
      print("Not implemented yet!")
      exit(1)
    else:
      show_useage(parser)
    try:
      pretty_print(response.json(), pretty = args.pretty)
    except ValueError:
        print("No data found")
  else:
    show_useage(parser)

def stream_records(url, data, params):
    response = requests.post(url, stream = True, json = data, params = params)
    for chunk in response.iter_content(chunk_size = None):
        yield chunk

def pretty_print(json_input, pretty = False, to_array = False):
  if pretty:
    output = json.dumps(json_input, indent=2)
  else:
    output = json.dumps(json_input)
  if to_array:
    output += ","
  print(output)

def show_useage(parser):
  parser.print_help()
  exit(1)

if __name__ == "__main__":
  main()
