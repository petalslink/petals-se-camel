# Petals and Apache Camel

See https://doc.petalslink.com/display/petalscomponents/Petals-SE-Camel+1.0.0-SNAPSHOT for Specifications and User Documentation.

The integration tests are in the Petals SVN and accessible at http://jenkins.petalslink.org.

## Architecture

There is two main projects in this repository:
* camel-petals
* petals-se-camel

### camel-petals

It implements a Camel Component that can be used in Camel in order to consumes (with `from("petals:..."`) and produces (with `to("petals:..."`) messages from and to Petals.

It needs to be provided with an implementation on how to communicate with Petals.
Currently there is only one implementation, that is petals-se-camel (see next section).

In the future, it could be possible to use Camel from outside Petals and be able to contact and existing Petals Container.

### petals-se-camel

It is a Petals Service Engine that embeds Apache Camel inside Petals.
It obviously uses (and implements parts of) camel-petals for the integration into Camel.
