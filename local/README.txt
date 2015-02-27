== Local configuration files ==

Developers: Place *.properties files in this folder to customize project 
for your development environment (port numbers, database, etc).

If a file [test|server|sitecontroller].log4j2.yml or log4j.yml exists, 
it will be used INSTEAD OF default logging configs in resources.

*.yml and *.properties files in this folder are read at runtime and are 
not checked in to git.
  