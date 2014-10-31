#! /bin/sh
LIQUIBASE_HOME=/usr/local/liquibase-3.2.2/
 
# build classpath from all jars in lib
  CP=.:target/build/compiled-classes
  for i in "$LIQUIBASE_HOME"/liquibase*.jar; do
    CP="$CP":"$i"
  done
  for i in "$LIQUIBASE_HOME"/lib/*.jar; do
    CP="$CP":"$i"
  done
  for i in lib/dist/*.jar; do
    CP="$CP":"$i"
  done

# add any JVM options here
JAVA_OPTS="${JAVA_OPTS-}"
echo classpath = ${CP}
java -cp "$CP" $JAVA_OPTS liquibase.integration.commandline.Main ${1+"$@"}


