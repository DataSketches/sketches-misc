[![][travis img]][travis]
[![][mavenbadge img]][mavenbadge] [![][versioneye img]][versioneye]

# sketches-misc

Demos, characterization testing and other code not related to production deployment.  

This code is offered "as is".  It has been written mostly as "hacks" to get a particular job done, 
does not have unit tests, and was never intended to survive the original objectives. 
Nonetheless, some folks have found it useful.  If you find it useful, go for it. 

### [Memory Performance Experiments](https://github.com/DataSketches/sketches-misc/blob/master/docs/MemoryPerformance.md)

### "sketch" Command Line Capability

The core sketch library can be used from a command line with the simple CommandLine parser included. 
This has limited utility as it can only be run on a single machine, while the DataSketches library 
was primarily writen for large-scale distributed systems.

[travis]:https://travis-ci.org//DataSketches/sketches-misc/builds?branch=master
[travis img]:https://secure.travis-ci.org/DataSketches/sketches-misc.svg?branch=master

[mavenbadge]:http://search.maven.org/#search|gav|1|g%3A%22com.yahoo.datasketches%22%20AND%20a%3A%22sketches-misc%22
[mavenbadge img]:https://maven-badges.herokuapp.com/maven-central/com.yahoo.datasketches/sketches-misc/badge.svg

[versioneye]:https://www.versioneye.com/user/projects/5751f0527757a00041b3a1fb
[versioneye img]:https://www.versioneye.com/user/projects/5751f0527757a00041b3a1fb/badge.svg?style=flat
