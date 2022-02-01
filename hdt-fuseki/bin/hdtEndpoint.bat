@echo off

call "%~dp0\javaenv.bat"

"%JAVACMD%" %JAVAOPTIONS% -classpath "%~dp0\..\lib\*" "org.apache.jena.fuseki.main.cmds.FusekiHDTCmd" %*
