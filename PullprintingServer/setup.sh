#!/bin/bash
echo "setup script is running..."

echo "start cups..."
sudo /bin/systemctl start cups

FILE=/tmp/cloud-print-connector-monitor.sock
if [ -S $FILE ]; then
   echo "File $FILE exists... removing it"
   rm /tmp/cloud-print-connector-monitor.sock
fi

echo "File $FILE does not exist... start gcp-cups-connector"
gcp-cups-connector -config-filename gcp-cups-connector.config.json &

until [[ -e $FILE ]] ;do
    echo "waiting socket gcp-cups-connector creation..."
    sleep 3
done

#echo "stop cups..."
#sudo /bin/systemctl stop cups

SERVICE="node"
if pgrep -x "$SERVICE" >/dev/null
then
    echo "$SERVICE is running..."
else
    echo "$SERVICE stopped... start node"
    node FBK\ Print\ API\ Nodejs\ NOReSFRESH/server.js &
fi

echo "setup script finished..."

