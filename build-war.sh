#!/bin/bash
if [ ! -d formatter ]; then
  mkdir formatter
  if [ $? -ne 0 ] ; then
    echo "couldn't create formatter directory"
    exit
  fi
fi
if [ ! -d formatter/WEB-INF ]; then
  mkdir formatter/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create formatter/WEB-INF directory"
    exit
  fi
fi
if [ ! -d formatter/WEB-INF/lib ]; then
  mkdir formatter/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create formatter/WEB-INF/lib directory"
    exit
  fi
fi
rm -f formatter/WEB-INF/lib/*.jar
cp dist/Formatter.jar formatter/WEB-INF/lib/
cp web.xml formatter/WEB-INF/
jar cf formatter.war -C formatter WEB-INF 
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
