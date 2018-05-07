from maven:3-jdk-8

COPY . /usr/local/src/hdt-java

RUN cd /usr/local/src/hdt-java \
  && mvn install \
  && cd hdt-java-package \
  && mvn assembly:single \
  && mv target/hdt-java-*/hdt-java-* /opt/hdt-java

ENV PATH="/opt/hdt-java/bin:${PATH}"

CMD ["/bin/echo", "Available commands: hdt2rdf.sh hdtInfo.sh hdtSearch.sh hdtsparql.sh rdf2hdt.sh"]
