## Overview

Project contains [Jackson](http://http://wiki.fasterxml.com/JacksonHome) extension component
for reading and writing [Protobuf](http://code.google.com/p/protobuf/) encoded data (see
[protobuf encoding spec](https://developers.google.com/protocol-buffers/docs/encoding)).
This project adds necessary abstractions on top to make things work with other Jackson functionality;
mostly just low-level Streaming components (`JsonFactory`, `JsonParser`, `JsonGenerator`).

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Dependencies

Protoc (protobuf IDL) parsing is done using [square/protoparser](https://github.com/square/protoparser) library.
(note: another library, [Protostuff](http://code.google.com/p/protostuff/), also has usable parser)

Project does NOT depend on the official [Google Java protobuf](https://github.com/google/protobuf) library, although
it may be used for conformance testing purposes in future.

Protobuf module requires Java 7, due to `protoparser` requiring Java 7.

# Status

[![Build Status](https://travis-ci.org/FasterXML/jackson-dataformat-protobuf.svg)](https://travis-ci.org/FasterXML/jackson-dataformat-protobuf)

With release 2.6.0 (Jul-2015), this module is considered stable, although number of production deployments is still limited.

## Functionality

### Supported versions

Version 2 of `protoc` supported. This is the official standard used in production as of Aug-2015.
There is work underway by protobuf authors to specify a new version, called 'v3', but it has
not yet been finalized, although draft versions exist.

Protoc schema parser supports v3 as well (that is, protoc schemas
can be read in), but encoder/decoder does not support new v3 features.
This means that module will not encode (write) data using v3 constructs, nor be able to decode (read)
v3 encoded data.

When v3 specification is finalized we are likely to work on upgrading module to support v3; either
as option of this module, or via new v3-based module, depending on exact compatibility details
between v2 and v3.

### Missing features

Following features are not yet fully implemented as of version 2.6, but are planned to be evetually supported;

* Enforcing of mandatory values
* Value defaulting
* Construction of `protoc` schemas using alternatives to reading textual definition; for example, programmatic construction, or generation from Java classes.

