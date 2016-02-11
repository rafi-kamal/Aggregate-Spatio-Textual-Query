#!/bin/bash

 gnuplot -e 'indir="/home/rafi/Dropbox/Thesis/Results/yelp/cpu"; outdir="~/Desktop/Writing/graphs/yelp/cpu"; ylabel="running time (ms)"; offset=200' plot.gpl
 gnuplot -e 'indir="/home/rafi/Dropbox/Thesis/Results/yelp/io"; outdir="~/Desktop/Writing/graphs/yelp/io"; ylabel="\\# page accesses"; offset=1000' plot.gpl
 gnuplot -e 'indir="/home/rafi/Dropbox/Thesis/Results/flickr/cpu"; outdir="~/Desktop/Writing/graphs/flickr/cpu"; ylabel="running time (ms)"; offset=10000' plot.gpl
 gnuplot -e 'indir="/home/rafi/Dropbox/Thesis/Results/flickr/io"; outdir="~/Desktop/Writing/graphs/flickr/io"; ylabel="\\# page accesses"; offset=1000' plot.gpl