#!/bin/bash

# -------------------------------------------------------------------------------
#  Please adapt according to your system
# -------------------------------------------------------------------------------
export SOFTWARE_DIR=/home/waqss/software/ceres-metadata-[version]/
export CLASSPATH=$SOFTWARE_DIR:$SOFTWARE_DIR/lib/*

java -cp "$CLASSPATH" com.bc.ceres.standalone.MetadataEngineMain "$@"