#! /bin/sh

export BEAM4_HOME=${installer:sys.installationDir}

if [ -z "$BEAM4_HOME" ]; then
    echo
    echo Error: BEAM4_HOME not found in your environment.
    echo Please set the BEAM4_HOME variable in your environment to match the
    echo location of the BEAM 4.x installation
    echo
    exit 2
fi

export PATH=$PATH:$BEAM4_HOME/bin

echo ""
echo "Welcome to the BEAM command-line interface!"
echo "The following command-line tools are available:"
echo "  gpt.sh            - General Graph Processing Tool"
echo "  pconvert.sh       - General product conversion and quicklook generation"
echo "  binning.sh        - General level 3 binning processor"
echo "  flhmci.sh         - General FLH / MCI processor"
echo "  meris-cloud.sh    - Envisat/MERIS cloud probability processor"
echo "  meris-smac.sh     - Envisat/MERIS atmospheric correction (SMAC)"
echo "  aatsr-sst.sh      - Envisat/AATSR sea surface temperaure processor"
echo "  visat-d.sh        - VISAT application launcher for debugging"
echo "Typing the name of the tool will output its usage information."
echo ""
