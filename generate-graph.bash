#!/bin/bash

# Combines the stats for sum and max in a single file
# $1 = directory
function combine {
	dir=$1

	mkdir -p "combined-data/cpu"
	mkdir -p "combined-data/io"

	for path in $(ls $dir"/max/cpu"); do
		file=$(basename $path)
		paste $dir'/sum/cpu/'$file $dir'/max/cpu/'$file | awk '{print $1,$3,$2,$6,$5}' \
			> 'combined-data/cpu/'$file 
		paste $dir'/sum/io/'$file $dir'/max/io/'$file | awk '{print $1,$3,$2,$6,$5}' \
			> 'combined-data/io/'$file
	done
}

indir="/home/rafi/Dropbox/Thesis/Results"
outdir="graphs"

mkdir -p $outdir"/flickr/io"
mkdir -p $outdir"/flickr/cpu"
mkdir -p $outdir"/yelp/io"
mkdir -p $outdir"/yelp/cpu"

mkdir -p combined-data

combine $indir"/yelp"
gnuplot -e 'indir="combined-data/cpu"; 	outdir="'$outdir'/yelp/cpu"; ylabel="running time (ms)"; offset=300' plot.gpl
gnuplot -e 'indir="combined-data/io";  	outdir="'$outdir'/yelp/io";  ylabel="\\# page accesses"; offset=1000' plot.gpl

combine $indir"/flickr"
gnuplot -e 'indir="combined-data/cpu"; 	outdir="'$outdir'/flickr/cpu"; ylabel="running time (ms)"; offset=5000' plot.gpl
gnuplot -e 'indir="combined-data/io";  	outdir="'$outdir'/flickr/io";  ylabel="\\# page accesses"; offset=500' plot.gpl

function combineKeywordDropping {
	dir=$1

	file=$2
	paste $dir'/sum/'$file $dir'/max/'$file | awk '{print $1,$2,$4}' \
		> 'combined-data/'$file 
}

combineKeywordDropping $indir"/yelp" "keyword-dropping-cost.dat"
gnuplot -e 'indir="combined-data";	outdir="'$outdir'/yelp/";	ylabel="Query Cost";	offset=0' plot-dropping-cost.gpl
combineKeywordDropping $indir"/flickr"
gnuplot -e 'indir="combined-data";	outdir="'$outdir'/flickr/";	ylabel="Query Cost";	offset=0' plot-dropping-cost.gpl

combineKeywordDropping $indir"/yelp" "keyword-dropping-time.dat"
gnuplot -e 'indir="combined-data";	outdir="'$outdir'/yelp/";	ylabel="Query Cost";	offset=0' plot-dropping-time.gpl
combineKeywordDropping $indir"/flickr"
gnuplot -e 'indir="combined-data";	outdir="'$outdir'/flickr/";	ylabel="Query Cost";	offset=0' plot-dropping-time.gpl

# rm -rf $writingLocation
cp -r $outdir "/home/rafi/Desktop/Writing/"

rm -rf "combined-data"