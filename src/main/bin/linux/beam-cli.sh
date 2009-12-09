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
echo "  mapproj.sh        - General map projections"
echo "  binning.sh        - General level 3 binning processor"
echo "  mosaic.sh         - General level 3 mosaicing processor"
echo "  flhmci.sh         - General FLH / MCI processor"
echo "  meris-rad2refl.sh - Envisat/MERIS radiiance to reflectance processor"
echo "  meris-smac.sh     - Envisat/MERIS atmospheric correction (SMAC)"
echo "  meris-smac.sh     - Envisat/MERIS radiance to reflectance processor"
echo "  aatsr-sst.sh      - Envisat/AATSR sea surface temperaure processor"
echo "  visat-d.sh        - VISAT application launcher for debugging"
echo "Typing the name of the tool will output its usage information."
echo ""
