#!/bin/sh

os_version=`uname -r`
machine_platform=`uname -p`
if [ "${os_version:0:1}" == "6" ];then
    executable="osx-10.2"
elif [ "${machine_platform}" == "i386" ];then
    executable="osx-intel"
else
    executable="osx-ppc"
fi

if [ "$executable" == "none" ]; then
    echo "The current OS X version is not supported"
    exit 1
fi
            
        "`dirname \"${0}\"`/$executable" "$@"