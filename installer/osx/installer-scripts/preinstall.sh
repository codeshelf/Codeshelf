#!/bin/sh

PID=`/bin/ps auxww | grep -v grep | grep $USER | grep loginwindow | awk '{print $2}'`

launchctl bsexec $PID chroot -u $USER / launchctl unload /Library/LaunchAgents/com.gadgetworks.CodeShelf.AutoRun.plist

if [ ! -d "/Library/Application Support/CodeShelf" ]
then
	logger "/Library/Application Support/CodeShelf"
	sudo mkdir "/Library/Application Support/CodeShelf"
	sudo chown root "/Library/Application Support/CodeShelf"
	sudo chgrp wheel "/Library/Application Support/CodeShelf"
	sudo chmod 755 "/Library/Application Support/CodeShelf"
fi

if [ ! -d /usr/local ]
then
	logger "Creating /usr/local"
	sudo mkdir /usr/local
	sudo chown root /usr/local
	sudo chgrp wheel /usr/local
	sudo chmod 755 /usr/local
fi

if [ ! -d /usr/local/lib ]
then
	logger "Creating /usr/local/lib"
	sudo mkdir /usr/local/lib
	sudo chown root /usr/local/lib
	sudo chgrp wheel /usr/local/lib
	sudo chmod 755 /usr/local/lib
fi