#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME="gradlew"

PRG="$0"
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")/"$link"
    fi
done

SAVED=$(pwd)
cd $(dirname "$PRG") >/dev/null
APP_HOME=$(pwd -P)
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
JVM_OPTS=$DEFAULT_JVM_OPTS
JAVA_OPTS=$DEFAULT_JAVA_OPTS

JAVACMD="java"
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is not defined correctly."
    echo "We cannot execute $JAVACMD"
    exit 1
fi

exec "$JAVACMD" $JVM_OPTS $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"