#!/bin/bash

if [ $# -lt 1 ]; then
	echo -e "USAGE: buildtree.bash data_directory [-d]"
	exit 0
fi

cd $1

# Delete all generated files
if [ "$2" == "-d" ]; then
	find . -type f -not -name '*.txt' | xargs rm
fi

runjava="java -ea -Dfile.encoding=UTF-8 -classpath ../bin:../lib/jdbm-1.0.jar"

# Preparing and building trees
$runjava build.StoreDocument wwords.txt 4096
$runjava build.BuildRtree loc.txt rtree 4096 100
$runjava build.BuildIRtree wwords.txt rtree 4096

# Output of the query
# echo
# tput setaf 2

# $runjava algorithm.knn.LKT rtree query.txt 5 0.5 0

# tput sgr0