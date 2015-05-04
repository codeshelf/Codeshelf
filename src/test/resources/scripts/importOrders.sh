# ./importOrders.sh simulate@example.com XXXXXX af44f88e-9569-48a3-b4db-d3b6e3c4689d ../perftest/orders-1.csv importedOrders.txt

USER=$1
PASS=$2
FACILITY=$3
FILE=$4
OUT_FILE=$5
HOST=localhost
COOKIE_JAR_PATH=/tmp/import_cookies
echo "Cookies are at: " $COOKIE_JAR_PATH
curl --include --cookie-jar $COOKIE_JAR_PATH --data "u=$USER&p=$PASS" http://$HOST:8181/auth/
# curl --include --cookie $COOKIE_JAR_PATH http://$HOST:8181/api/facilities
rm $OUT_FILE
curl -v --cookie $COOKIE_JAR_PATH  --form file=@$FILE http://$HOST:8181/api/facilities/$FACILITY/import/orders/ >> $OUT_FILE
