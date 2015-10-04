#!/usr/bin/python

import os
import sys
import json
import csv

if len(sys.argv) != 3:
	print "USAGE: parse.py input_file output_directory"

input_file = open(sys.argv[1], 'r')
location_file = open(os.path.join(sys.argv[2], 'loc.txt'), 'w')
keyword_file = open(os.path.join(sys.argv[2], 'words.txt'), 'w')

id = 0				# id of the data object
keyword_map = {}	# holds keyword string and corresponding integer id

reader = csv.reader(input_file, delimiter = '\t')
for keyword_list, latitude, longitude in reader:
	if keyword_list and latitude and longitude:
		# print keywords, latitude, longitude

		keywords = keyword_list.split(',')
		for keyword in keywords:
			if keyword not in keyword_map:
				# convert keyword string to integer ids
				keyword_map[keyword] = len(keyword_map)

		keyword_ids = [keyword_map[keyword] for keyword in keywords]

		location_file.write(','.join(str(x) for x in [id, latitude, longitude]) + "\n")
		keyword_file.write(','.join(str(x) for x in [id] + keyword_ids) + "\n")

		id += 1

print id, len(keyword_map)