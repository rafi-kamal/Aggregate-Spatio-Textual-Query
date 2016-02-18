#!/bin/bash

aggregate="max"

gnuplot -e 'indir="~/Dropbox/Thesis/Results/yelp/'$aggregate'/cpu"; outdir="~/Desktop/Writing/graphs/yelp/cpu"; ylabel="running time (ms)"; offset=200' plot.gpl
gnuplot -e 'indir="~/Dropbox/Thesis/Results/yelp/'$aggregate'/io"; outdir="~/Desktop/Writing/graphs/yelp/io"; ylabel="\\# page accesses"; offset=1000' plot.gpl
# gnuplot -e 'indir="~/Dropbox/Thesis/Results/flickr/'$aggregate'/cpu"; outdir="~/Desktop/Writing/graphs/flickr/cpu"; ylabel="running time (ms)"; offset=10000' plot.gpl
# gnuplot -e 'indir="~/Dropbox/Thesis/Results/flickr/'$aggregate'/io"; outdir="~/Desktop/Writing/graphs/flickr/io"; ylabel="\\# page accesses"; offset=1000' plot.gpl