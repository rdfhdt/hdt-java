set JAVAOPTIONS=-Xmx1G
set JAVACMD=java

set JAVACP="%~dp0\..\target;%~dp0\..\target\classes;%~dp0\..\target\dependency\*.jar;.

 for /R %~dp0\..\target\dependency %%a in (*.jar) do (
   set JAVACP=!JAVACP!;%%a
 )
set JAVACP=!JAVACP!"
