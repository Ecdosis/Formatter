#!/bin/bash
service tomcat6 stop
cp formatter.war /var/lib/tomcat6/webapps/
rm -rf /var/lib/tomcat6/webapps/formatter
rm -rf /var/lib/tomcat6/work/Catalina/localhost/
service tomcat6 start
