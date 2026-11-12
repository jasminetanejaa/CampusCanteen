#!/bin/sh
mkdir -p out && javac -d out src/canteen/*.java && java -cp out canteen.CanteenApp
