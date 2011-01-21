#!/bin/sh

PID=`/bin/ps auxww | grep -v grep | grep $USER | grep loginwindow | awk '{print $2}'`

launchctl bsexec $PID  chroot -u $USER / launchctl load /Library/LaunchAgents/com.gadgetworks.CodeShelf.AutoRun.plist
#launchctl bsexec $PID  chroot -u $USER / launchctl start com.gadgetworks.CodeShelf