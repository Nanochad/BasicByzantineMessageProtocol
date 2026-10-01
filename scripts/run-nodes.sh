#!/bin/bash
# Launches 4 terminals, one per node, running lab1-1.0.jar

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CMD="java -Dlog4j2.configurationFile=./log4j2-base.xml -jar target/lab1-1.0.jar crypto_name="

for i in 1 2 3 4; do
    gnome-terminal --working-directory="$DIR" -- bash -c "${CMD}node${i}; exec bash"
done
