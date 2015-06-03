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

(Apr-2014): Implementation complete, ready for 2.6.0-SNAPSHOT!

## Functionality

### Supported versions

Version 2 of `protoc` supported; parser supports v3 as well but encoder/decoder does not.

### Missing features

Following features are not yet fully implemented, but are planned to be evetually supported;

* Enforcing of mandatory values
* Value defaulting

