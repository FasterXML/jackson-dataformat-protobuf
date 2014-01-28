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

# Status

(Jan-2014): Still in initial prototyping phase:

 * Basic integration of `protoparser` exists, looks like we can read protoc stuff as expected. * Starting to work on `ProtobufGenerator` first, seems slightly simpler
    * one complex part: protobuf uses length-prefixing, which is a major PITA for embedded messages
