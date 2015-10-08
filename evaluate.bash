#!/bin/bash

# Run the experiments with different parameters and collect performance evaluation data

if [ $# -lt 2 ]; then
	echo -e "USAGE: evalulate.bash input_directory output_directory"
	exit 0
fi

# delete all output files
rm $2/*

runjava="java -ea -Dfile.encoding=UTF-8 -classpath ./bin:./lib/jdbm-1.0.jar"
queryTypes=(1 2 3 4 5 6)

# Group Size
ns=(10 20 40 70)
nDefault=20

mPercentages=(40 50 65 80)
mPercentageDefault=50

numberOfKeywords=(2 4 8 16)
numberOfKeywordsDefault=4

querySpaceAreaPercentages=(1 2 4 8 16)
querySpaceAreaPercentageDefault=4

keywordSpaceSizePercentages=(1 2 4 8 16)
keywordSpaceSizePercentageDefault=4

topks=(1 5 10 20 40)
topkDefault=5

alphas=(.1 .3 .5 .7 .9)
aplhaDefault=.5

for n in ${ns[@]}; do
	run $1 $n $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault
	write $n $2/groupsize
done

for mPercentage in ${mPercentages[@]}; do
	run $1 $nDefault $mPercentage $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault
	write $n $2/subgroupSize
done

for numberOfKeyword in ${numberOfKeywords[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeyword $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault
	write $n $2/numberOfKeyword
done

for querySpaceAreaPercentage in ${querySpaceAreaPercentages[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentage \\
		$keywordSpaceSizePercentageDefault $topkDefault $aplhaDefault
	write $n $2/querySpaceAreaPercentage
done

for keywordSpaceSizePercentage in ${keywordSpaceSizePercentages[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentage $topkDefault $aplhaDefault
	write $n $2/keywordSpaceSizePercentage
done

for topk in ${topks[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentageDefault $topk $aplhaDefault
	write $n $2/topk
done

for alpha in ${alphas[@]}; do
	run $1 $nDefault $mPercentageDefault $numberOfKeywordsDefault $querySpaceAreaPercentageDefault \\
		$keywordSpaceSizePercentageDefault $topkDefault $alpha
	write $n $2/alpha
done

# $1 = directory
# $2 = n
# $3 = mPercentage
# $4 = numberOfKeywords
# $5 = querySpacePercentage
# $6 = keywordSpacePercentage
# $7 = topk
# $8 = alpha
function run {
	# Generate queries
	$runjava test.QueryGenerator $1 $2 $3 $4 $5 $6

	# Run algorithms and collect result
	local cpuCosts=()
	local ioCosts=()
	for queryType in ${queryTypes[@]}; do
		result=$($runjava test.Main $1/rtree $1/gnnk.txt $1/sgnnk.txt $7 $8 $queryType)
		cpuCosts+=${result[0]}
		ioCosts+=${result[1]}
	done

	gnnkcpu="${cpuCosts[2]} ${cpuCosts[1]}"
	gnnkio="${ioCosts[2]} ${ioCosts[1]}"
	sgnnkcpu="${cpuCosts[4]} ${cpuCosts[3]}"
	sgnnkio="${ioCosts[4]} ${ioCosts[3]}"
	sgnnkecpu="${cpuCosts[6]} ${cpuCosts[5]}"
	sgnnkeio="${ioCosts[6]} ${ioCosts[5]}"
}

function write {
	key=$1
	filePrefix=$2
	echo $key $gnnkcpu >> $filePrefix-gnnk-cpu.dat
	echo $key $gnnkio >> $filePrefix-gnnk-io.dat
	echo $key $sgnnkcpu >> $filePrefix-sgnnk-cpu.dat
	echo $key $sgnnkio >> $filePrefix-sgnnk-io.dat
	echo $key $sgnnkecpu >> $filePrefix-sgnnke-cpu.dat
	echo $key $sgnnkecpu >> $filePrefix-sgnnke-cpu.dat
}