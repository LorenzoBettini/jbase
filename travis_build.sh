#!/bin/bash

set -ev
if [ "$TRAVIS_OS_NAME" == "osx" ]; then
	if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
		echo "Build on MacOSX: Pull Request"
		mvn -f jbase.releng/pom.xml clean verify -Dtycho.disableP2Mirrors=true
	else
		echo "Skipping build on MacOSX for standard commit"
	fi
else
	echo "Build on Linux"
	mvn -f jbase.releng/pom.xml clean verify -Pjacoco-report coveralls:report -Dtycho.disableP2Mirrors=true
fi 
