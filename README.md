tinylog2-humio
============

- `tinylog2-humio` is a [Writer](https://tinylog.org/v2/extending/#custom-writer) implementation for
[tinylog2](https://www.tinylog.org/v2/) writing log messages to a [HUMIO](https://www.humio.com/) using [Ingest API](https://docs.humio.com/api/ingest/)

- library supports `gzip` compression and batch write triggerd by either interval or minimum events

- tiny library with minimal dependencies 

- using [stream-batch-interval](https://github.com/chaudhryfaisal/stream-batch-interval) - for batching 
- using [humio-java-client](https://github.com/chaudhryfaisal/humio-java-client) - as java client 


Configuration
-------------

The following configuration settings are supported by `tinylog2-humio`:

    private Set<LogEntryValue> requiredLogEntryValues;
    private Map<String, Object> staticFields;

    private int batchActions = BatchProcessor.DEFAULT_ACTION_SIZE;
    private int batchFlushInterval = BatchProcessor.DEFAULT_FLUSH_INTERVAL;
    private int batchBufferLimit = BatchProcessor.DEFAULT_BATCH_SIZE;
    private int batchJitterInterval = BatchProcessor.DEFAULT_JITTER_INTERVAL;
    private String serverEndpoint = Humio.DEFAULT_HOST_NAME;
    private String serverUri = Humio.DEFAULT_PATH;
    private String serverToken;

* `serverEndpoint` (default: `https://cloud.humio.com`)
  * humio server endpoint
  * this value can also be override by `-DHUMIO_SERVER_ENDPOINT` or `HUMIO_SERVER_ENDPOINT` as ENVIRONMENT variable
* `serverToken` (default: `EMPTY`)
  * [ingest token](https://docs.humio.com/ingesting-data/ingest-tokens/) 
    * this value can also be override by `-DHUMIO_SERVER_TOKEN` or `HUMIO_SERVER_TOKEN` as ENVIRONMENT variable
* `serverUri` (default: `/api/v1/ingest/humio-structured`)
  * full path for ingestion api
* `hostname` (default: local hostname or `localhost` as fallback)
  * The hostname of the application.
  * this value can also be override by `-DHUMIO_HOSTNAME` or `HUMIO_HOSTNAME` as ENVIRONMENT variable
* `additionalLogEntryValues` (default: `DATE,LEVEL,RENDERED_LOG_ENTRY`)
  * Additional information for log messages, see [`LogEntryValue`](http://www.tinylog.org/v2/javadoc/org/pmw/tinylog/writers/LogEntryValue.html).
* `staticFields` (default: `EMPTY`)
  * Additional static fields/tags to be attached to ever log entry 
* `batchActions` (default: `100`)
  * size of events if reached should flush to backend (set to `1` to flush evert log entry.. caution 1 log event 1 http call)
* `batchFlushInterval` (default: `5000`)
  * interval (ms) if reached should flush to backend
* `batchBufferLimit` (default: `1000`)
  * max size of events to submit in one http call

Additional configuration settings are supported by the `HumioWriter` class. Please consult the Javadoc for details.

Examples
--------

`tinylog2-humio` can be configured using a [configuration-file](https://tinylog.org/v2/configuration/#configuration). 

Properties file example:

    writer2=humio
    writer2.serverToken=INGESTION_TOKEN_HERE
    writer2.hostname=fancy-app
    writer2.staticFields=#type:tinylog2
    writer2.format={level}: {class} - {message}
    writer2.additionalLogEntryValues=EXCEPTION,FILE,LINE



Maven Artifacts
---------------

This project is available on Maven Central. To add it to your project simply add the following dependencies to your
`pom.xml`:

    <dependency>
      <groupId>com.github.chaudhryfaisal</groupId>
      <artifactId>tinylog2-humio</artifactId>
      <version>1.0.0</version>
    </dependency>


Humio Parser
-------

You could also add `tinylog2` log parser (same as `#type` from `writer2.staticFields=#type:tinylog2`) to parseand cleanup logs. [Read More: ](https://docs.humio.com/ref/creating-a-parser)

    parseJson() | 
    @rawstring := message | 
    drop([message]) |
    kvParse()

Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/chaudhryfaisal/tinylog2-humio/issues).


License
-------

Copyright (c) 2020 Faisal Chaudhry

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.
