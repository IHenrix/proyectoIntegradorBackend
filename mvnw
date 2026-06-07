#!/bin/sh
set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(dirname "$0")}"
MVNW_REPOURL="${MVNW_REPOURL:-https://repo.maven.apache.org/maven2}"

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ -r "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" ]; then
    DISTRIBUTION_URL=$(grep distributionUrl "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | sed -e 's/distributionUrl=//')
fi

if [ ! -e "$WRAPPER_JAR" ]; then
    wrapperJarUrl="${MVNW_REPOURL}/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    while IFS="=" read -r key value; do
        case "$key" in
            wrapperUrl) wrapperJarUrl="$value" ;;
        esac
    done < "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
    curl -o "$WRAPPER_JAR" "$wrapperJarUrl" -f -L || \
    wget -O "$WRAPPER_JAR" "$wrapperJarUrl"
fi

exec "$JAVA_HOME/bin/java" \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    $MAVEN_OPTS \
    $MVNW_OPTS \
    $WRAPPER_LAUNCHER "$@"
