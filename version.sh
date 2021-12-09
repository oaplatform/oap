#!/bin/bash

set -x

BUILD_COUNTER=$1
PROJECT_NAME=$2
BRANCH_NAME=$3

VERSION_XENOSS=$(grep -oP 'project\.version\>\K[^<]*' pom.xml)

if [ "$BRANCH_NAME" == "master" ] || [ "$BRANCH_NAME" == "" ] || [ "$BRANCH_NAME" == "refs/heads/master" ]; then
  VERSION_BRANCH=""
  MAVEN_BUILD_COUNTER=""
else
  VERSION_BRANCH="-${BRANCH_NAME}"
  MAVEN_BUILD_COUNTER="-${BUILD_COUNTER}"
fi

set +x

#project name
echo "##teamcity[setParameter name='oap.project.name' value='${PROJECT_NAME,,}']"

#maven master
echo "##teamcity[setParameter name='${PROJECT_NAME,,}.project.version' value='${VERSION_XENOSS}']"

#maven branch
echo "##teamcity[setParameter name='oap.project.version.branch' value='${VERSION_XENOSS}${VERSION_BRANCH}${MAVEN_BUILD_COUNTER}']"

#teamcity
set +x

echo "##teamcity[buildNumber '${VERSION_XENOSS}${VERSION_BRANCH}-${BUILD_COUNTER}']"
