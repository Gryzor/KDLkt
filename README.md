# KDLkt
This KDL Parser is a _mostly_ direct port (to Kotlin) of `kdl4j`. [The original project](https://github.com/kdl-org/kdl4j) is written in Java, so I decided to migrate it to Kolin because I'm super fun at parties...
The end result is _mostly_ a direct copy in almost every aspect (at least in this initial version) of the original code, with the **Copyright 2022 Hannah Kolbeck**.

In fact, the entire test suite of [the original project](https://github.com/kdl-org/kdl4j) was copy-pasted directly into this project (with minor modifications) to ensure that the new version of the parser was working fine after _the port_. All tests are green.

In other words, when I decided to use KDL, I had no real good reason (I could have used JSON), and instead of trying to build a parser myself, I decided to "copy" kdl4j and make it Kotlin for no good reason either. 

The Search aspect of the project is *not yet ported* (or tested).

See original [README.md](https://github.com/kdl-org/kdl4j/blob/trunk/README.md) for more information.


### Original Parser
https://github.com/kdl-org/kdl4j

### Copyright of the Architecture, and Unit Tests
Copyright 2022 Hannah Kolbeck

This project is also MIT licensed. 

