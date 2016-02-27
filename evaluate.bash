#!/bin/bash

# Run the experiments with different parameters and collect performance evaluation data

if [ $# -lt 3 ]; then
	echo -e "USAGE: evalulate.bash input_directory output_directory aggregator"
	exit 0
fi

# delete all output files
rm -rf $2/*
mkdir -p $2/cpu/
mkdir -p $2/io/

runjava="java -ea -Dfile.encoding=UTF-8 -classpath ./bin:./lib/jdbm-1.0.jar"
queryTypes=(5 4 3 2 1 0)

# Group Size
ns=(10 20 40 60 80)
nDefault=10

mPercentages=(40 50 60 70 80)
mPercentageDefault=60

numberOfKeywords=(1 2 3 5 8 10)
numberOfKeywordsDefault=4

querySpaceAreaPercentages=(1 5 10 20 30 40)
querySpaceAreaPercentageDefault=20

keywordSpaceSizePercentages=(1 5 10 20 30 40)
keywordSpaceSizePercentageDefault=20

topks=(1 10 20 30 40 50)
topkDefault=10

alphas=(0.0 0.3 0.5 0.7 1.0)
aplhaDefault=0.5

# $1 = directory
# $2 = n
# $3 = mPercentage
# $4 = numberOfKeywords
# $5 = querySpacePercentage
# $6 = keywordSpacePercentage
# $7 = topk
# $8 = alpha
# $9 = query type
# $0 = aggregator
function run {
	# Generate queries
	$runjava test.QueryGenerator $1 $2 $3 $4 $5 $6 $9

	# Run algorithms and collect result
	local cpuCosts=()
	local ioCosts=()
	for queryType in ${queryTypes[@]}; do
		result=($($runjava test.Main $1/rtree $1/gnnk.txt $1/sgnnk.txt $7 $8 $queryType))
		cpuCosts[queryType]=${result[0]}
		ioCosts[queryType]=${result[1]}
	done

	gnnkcpu="${cpuCosts[0]} ${cpuCosts[1]}"
	gnnkio="${ioCosts[0]} ${ioCosts[1]}"
	sgnnkcpu="${cpuCosts[2]} ${cpuCosts[3]}"
	sgnnkio="${ioCosts[2]} ${ioCosts[3]}"
	sgnnkecpu="${cpuCosts[4]} ${cpuCosts[5]}"
	sgnnkeio="${ioCosts[4]} ${ioCosts[5]}"
}

# $1 = key
# $2 = output directory
# $3 = output file prefix
function writeData {
	echo "Writing data for $1 in $3..."
	echo $1 $gnnkcpu | tee -a "$2/cpu/$3-gnnk.dat"
	echo $1 $gnnkio | tee -a "$2/io/$3-gnnk.dat"
	echo $1 $sgnnkcpu | tee -a "$2/cpu/$3-sgnnk.dat"
	echo $1 $sgnnkio | tee -a "$2/io/$3-sgnnk.dat"
	echo $1 $sgnnkecpu | tee -a "$2/cpu/$3-sgnnke.dat"
	echo $1 $sgnnkeio | tee -a "$2/io/$3-sgnnke.dat"
}



for n in ${ns[@]}; do
	run $1 $n $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault $3
	writeData $n $2 groupsize
done

for mPercentage in ${mPercentages[@]}; do
	run $1 $nDefault $mPercentage $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault $3
	writeData $mPercentage $2 subgroup-size
done

for numberOfKeyword in ${numberOfKeywords[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeyword $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault $3
	writeData $numberOfKeyword $2 number-of-keyword
done

for querySpaceAreaPercentage in ${querySpaceAreaPercentages[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentage \
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault $3
	writeData $querySpaceAreaPercentage $2 query-space-area
done

for keywordSpaceSizePercentage in ${keywordSpaceSizePercentages[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentage $topkDefault $aplhaDefault $3
	writeData $keywordSpaceSizePercentage $2 keyword-space-size
done

for topk in ${topks[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentageDefault $topk $aplhaDefault $3
	writeData $topk $2 topk
done

for alpha in ${alphas[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \
		$keywordSpaceSizePercentageDefault $topkDefault $alpha $3
	writeData $alpha $2 alpha
done

# Keyword Dropping

droppingPercentages=(0 20 40 60 80)
$runjava test.QueryGenerator $1 $nDefault $mPercentageDefault 10 \
	$querySpaceAreaPercentageDefault $keywordSpaceSizePercentageDefault $3

for droppingPercentage in ${droppingPercentages[@]}; do
	result=($($runjava test.Main $1/rtree $1/gnnk.txt $1/sgnnk.txt $topkDefault $aplhaDefault 0 $droppingPercentage))
	cost=${result[2]}

	echo $droppingPercentage $cost | tee -a "$2/keyword-dropping.dat"
done

cp $2 ~/Desktop
