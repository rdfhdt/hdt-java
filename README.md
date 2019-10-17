[![Join the chat at https://gitter.im/rdfhdt](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/rdfhdt)

# HDT Library, Java Implementation. http://www.rdfhdt.org

## Overview

HDT-lib is a Java Library that implements the W3C Submission (http://www.w3.org/Submission/2011/03/) of the RDF HDT (Header-Dictionary-Triples) binary format for publishing and exchanging RDF data at large scale. Its compact representation allows storing RDF in fewer space, providing at the same time direct access to the stored information. This is achieved by depicting the RDF graph in terms of three main components: Header, Dictionary and Triples. The Header includes extensible metadata required to describe the RDF data set and details of its internals. The Dictionary organizes the vocabulary of strings present in the RDF graph by assigning numerical IDs to each different string. The Triples component comprises the internal structure of the RDF graph in a compressed form.

It provides several components:
- hdt-java-api: Abstract interface for dealing with HDT files.
- hdt-java-core: Core library for accessing HDT files programmatically from java. It allows creating HDT files from RDF and converting HDT files back to RDF. It also provides a Search interface to find triples that match a specific triple pattern.
- hdt-java-cli: Commandline tools to convert RDF to HDT, merge two HDT files and access HDT files from a terminal.
- hdt-jena: Jena integration. Provides a Jena Graph implementation that allows accessing HDT files as normal Jena Models. In turn, this can be used with Jena ARQ to provide more advanced searches, such as SPARQL, and even setting up SPARQL Endpoints with Fuseki.
- hdt-java-package: Generates a package with all the components and launcher scripts.
- hdt-fuseki: Packages Apache Jena Fuseki with the HDT jars and a fast launcher, to start a SPARQL endpoint out of HDT files very easily.


## Compiling

Use `mvn install` to let Apache Maven install the required jars in your system.

You can also run `mvn assembly:single` under hdt-java-package to generate a distribution directory with all the jars and launcher scripts.


## Usage

Please refer to hdt-java-package/README for more information on how to use the library. You can also find useful information on our Web Page http://www.rdfhdt.org


## License

Each module has a different License. Core is LGPL, examples and tools are Apache.

* `hdt-api`: Apache License
* `hdt-java-cli`: (Commandline tools and examples): Apache License
* `hdt-java-core`: Lesser General Public License
* `hdt-jena`: Lesser General Public License
* `hdt-fuseki`: Apache License


## Authors

* Mario Arias <mario.arias@gmailcom>
* Javier D. Fernandez <jfergar@infor.uva.es>
* Miguel A. Martinez-Prieto <migumar2@infor.uva.es>
* Dennis Diefenbach <dennis.diefenbach@univ-st-etienne.fr>
* Jose Gimenez Garcia: <jose.gimenez.garcia@univ-st-etienne.fr>

## Acknowledgements

RDF/HDT is a project developed by the Insight Centre for Data Analytics (www.insight-centre.org), University of Valladolid (www.uva.es), University of Chile (www.uchile.cl). Funded by Science Foundation Ireland: Grant No. SFI/08/CE/I1380, Lion-II; the Spanish Ministry of Economy and Competitiveness (TIN2009-14009-C02-02); Chilean Fondecyt's 1110287 and 1-110066; and the European Union's Horizon 2020 research and innovation program under the Marie Sklodowska-Curie grant agreement No 642795.
