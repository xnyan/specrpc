#!/usr/bin/python

import argparse
import json
import random
import string
import sys

argParser = argparse.ArgumentParser("")

argParser.add_argument(
    '-k', '--key', 
    dest='key', 
    nargs='?', 
    help='total number of keys',
    type=int,
    required=True)

argParser.add_argument(
    '-s', '--seed', 
    dest='seed', 
    nargs='?', 
    help='random seed; 0 for dynamic; default 1',
    type=int,
    default=1)

argParser.add_argument(
    '-d', '--data', 
    help='generates partition-distributed data file as init-data.json',
    action="store_true")

args = argParser.parse_args()
if args.seed != 0:
  random.seed(args.seed)

#key generation is cloned from TAPIR
charset = string.ascii_uppercase + string.ascii_lowercase + string.digits
keyDict = {}
dataTable = {}

print args.key
for i in range(args.key):
  rkey = "".join(random.choice(charset) for j in range(64))
  while rkey in keyDict:
    rkey = "".join(random.choice(charset) for j in range(64))
  keyDict[rkey] = True
  print rkey

  if args.data: 
    dataTable[rkey] = 0

if args.data:
  dataFile = open("init-data.json", 'w')
  json.dump(dataTable, dataFile)
  dataFile.close()
