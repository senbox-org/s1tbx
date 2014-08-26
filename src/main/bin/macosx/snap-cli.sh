#! /bin/sh

export S3TBX_HOME=${installer:sys.installationDir}

if [ -z "$S3TBX_HOME" ]; then
    echo
    echo Error: S3TBX_HOME not found in your environment.
    echo Please set the S3TBX_HOME variable in your environment to match the
    echo location of the S3TBX installation
    echo
    exit 2
fi

export PATH=$PATH:$S3TBX_HOME/bin

echo ""
echo "Welcome to the S3TBX command-line interface!"
echo "The following command-line tools are available:"
echo "  gpt.sh            - General Graph Processing Tool"
echo "  pconvert.sh       - General product conversion and quicklook generation"
echo "  visat-d.sh        - VISAT application launcher for debugging"
echo "Typing the name of the tool will output its usage information."
echo ""
