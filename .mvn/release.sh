#!/usr/bin/env bash

currentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`

if [[ "$currentVersion" == *"SNAPSHOT"* ]] ; then
  ./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
  developmentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
  ./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DnewVersion="$currentVersion"

  ./mvnw clean release:prepare release:perform -B \
      -Dgoals='package' -DgenerateReleasePoms=false -DgenerateBackupPoms=false \
      -DreleaseVersion="$currentVersion" -DdevelopmentVersion="$developmentVersion"
else
  git tag "v$currentVersion"

  ./mvnw build-helper:parse-version -DgenerateBackupPoms=false -DgenerateBackupPoms=false versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
  ./mvnw build-helper:parse-version -DgenerateBackupPoms=false -DgenerateBackupPoms=false versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion} \
    -f step-4-java-ee-jaxrs-jboss-spring-security

  git add . ; git commit -am "v$currentVersion release." ; git push --tags
fi
