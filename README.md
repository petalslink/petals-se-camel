# Petals and Apache Camel

[![Maven Central](https://img.shields.io/maven-central/v/org.ow2.petals/petals-se-camel.svg)]()
[![Jenkins](https://img.shields.io/jenkins/s/http/jenkins.petalslink.com/Petals Components - SE Camel.svg)](http://jenkins.petalslink.com/job/Petals Components - SE Camel/)
[![Jenkins Integration](https://img.shields.io/jenkins/s/http/jenkins.petalslink.com/SE_Camel_integration_tests.svg?label=integration)](http://jenkins.petalslink.com/job/SE_Camel_integration_tests/)
[![Jenkins tests](https://img.shields.io/jenkins/t/http/jenkins.petalslink.com/Petals Components - SE Camel.svg)](http://jenkins.petalslink.com/job/Petals Components - SE Camel/)
[![SonarQube Coverage](https://img.shields.io/sonar/http/sonar.petalslink.com/org.ow2.petals:petals-camel-parent/coverage.svg)](http://sonar.petalslink.com/?id=org.ow2.petals:petals-camel-parent)

## Specifications and User Documentation

See the Petals Wiki [page on the component](https://doc.petalslink.com/display/petalscomponents/Petals-SE-Camel+1.0.0-SNAPSHOT).

## Continuous Integration

* [Component](http://jenkins.petalslink.com/job/Petals%20Components%20-%20SE%20Camel/)
* [Integration Tests](http://jenkins.petalslink.com/job/SE_Camel_integration_tests/)

## Issues Reporting

* [Bug](https://jira.petalslink.com/secure/CreateIssue.jspa?pid=10230&issuetype=1)
* [New Feature](https://jira.petalslink.com/secure/CreateIssue.jspa?pid=10230&issuetype=2)
* [Improvement Request](https://jira.petalslink.com/secure/CreateIssue.jspa?pid=10230&issuetype=4)

## Architecture

There is two main projects in this repository.

### camel-petals

It implements a Camel Component that can be used in Camel in order to consumes (with `from("petals:..."`) and produces (with `to("petals:..."`) messages from and to Petals.

It needs to be provided with an implementation on how to communicate with Petals.
Currently there is only one implementation, that is petals-se-camel (see next section).

In the future, it could be possible to use Camel from outside Petals and be able to contact and existing Petals Container.

### petals-se-camel

It is a Petals Service Engine that embeds Apache Camel inside Petals.
It obviously uses (and implements parts of) camel-petals for the integration into Camel.

## Examples

There is currently three examples.

### su-camel-hello-proxy-java

A simple proxy that forwards a request to another service.
It is written in Java by implementing a Camel RouteBuilder.

### su-camel-hello-proxy-xml

A simple proxy that forwards a request to another service.
It is written in XML using the Camel Spring XML schema (but without using Spring itself).

### su-camel-datamapping

Again a proxy (in Java) but more advanced, it exploits the following features of Camel :
* Marshmalling and unmarshalling using JAXB.
* Mapping between two different service descriptions using a Java bean for the transformation.
* Logging inside the routes.
* Streamcaching (Petals uses stream to store exchange content).
