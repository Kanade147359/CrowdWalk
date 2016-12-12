#!/bin/sh

# export CROWDWALK=/path/to/CrowdWalk/crowdwalk
if test "$CROWDWALK" = "" ; then
    #	CROWDWALK='.'
    	CROWDWALK=`dirname $0`
fi

# カレントディレクトリの CrowdWalk を優先
if test "$(dirname $0)" = "." ; then
	DIR='.'
else
        DIR=$CROWDWALK
fi

JAVA='java'
JAVAOPT='-Dfile.encoding=UTF-8'
DYLD=$DIR/libs
JAR=$DIR/build/libs/crowdwalk.jar

OS=`uname -a`
case "$OS" in
    *"Darwin"*)
        echo " > Load Mac OS X libraries..."
        DYLD=$DIR/libs/macosx
        ;;
    *"CYGWIN"*"64"*)
        echo " > Load Windows amd64 libraries..."
        DYLD=$DIR/libs/windows/amd64
        ;;
    *"CYGWIN"*"i686"*)
        echo " > Load Windows i386 libraries..."
        DYLD=$DIR/libs/windows/i386
        ;;
    *"Linux"*"x86_64"*)
        echo " > Load linux amd64 libraries..."
        DYLD=$DIR/libs/linux/amd64
        ;;
    *"Linux"*"x86_32"*)
        echo " > Load linux i386 libraries..."
        DYLD=$DIR/libs/linux/i386
        ;;
    *)
        echo " > Current OS may not be supported..."
        echo " > Please check the architecture and libraries."
        exit 0
        ;;
esac

echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -jar $JAR $*"
$JAVA $JAVAOPT -Djava.library.path=$DYLD -Dprism.forceGPU=true -jar $JAR $*
