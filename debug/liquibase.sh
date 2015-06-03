#might take a little while so you can alway print it and replace here
LIQUIBASE_JAR=`find ~/.gradle/ -name "liquibase-core-3.2.2.jar" | head -1`
POSTGRES_JAR=`find ~/.gradle/ -name "postgresql-9.3-1100-jdbc41.jar" | head -1`
BASE_DIR=`dirname $0`/..

#read the properties files as underscored variables
. <(cat $BASE_DIR/src/main/resources/server.config.properties $BASE_DIR/local/server.config.properties \
    | awk -F= '/.*=.*/ {gsub(/\./, "_", $1); print $1 "=" $2 ";"}')
echo $shard_default_db_url

java -jar $LIQUIBASE_JAR \
    --classpath $POSTGRES_JAR \
    --changeLogFile=$BASE_DIR/src/main/resources/liquibase/db.changelog-master.xml \
    --url $shard_default_db_url \
    --username $tenant_default_username \
    --password $tenant_default_password \
    --defaultSchemaName $tenant_default_schema \
    "$@"
