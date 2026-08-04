Logback Elasticsearch Appender
===============================
![Last Build Status](https://github.com/TheDevOps/logback-elasticsearch-appender/actions/workflows/maven.yml/badge.svg?branch=master)

This project is a fork of https://github.com/internetitem/logback-elasticsearch-appender with several commits taken from not yet merged PRs, other forks and more.

There currently is not central release pipeline for it, we are just releasing it locally into our artifact repository. Feel free to fork or extend this fork as you see fit.

Send log events directly from Logback to Elasticsearch. Logs are delivered asynchronously (i.e. not on the main thread) so will not block execution of the program. Note that the queue backlog can be bounded and messages *can* be lost if Elasticsearch is down and either the backlog queue is full or the producer program is trying to exit (it will retry up to a configured number of attempts, but will not block shutdown of the program beyond that). For long-lived programs, this should not be a problem, as messages should be delivered eventually.

This software is dual-licensed under the EPL 1.0 and LGPL 2.1, which is identical to the [Logback License](http://logback.qos.ch/license.html) itself.

Usage
=====
Requires: Java 21+ and tested + compiled against logback 1.5.x and logback-access-common 2.0.x. Uses Jackson 3.

Include slf4j and logback as usual (depending on this library will *not* automatically pull them in).

In your `pom.xml` (or equivalent), add:

     <dependency>
        <groupId>at.porscheinformatik.crossng</groupId>
        <artifactId>logback-elasticsearch-appender</artifactId>
        <version>1.9-SNAPSHOT</version>
     </dependency>

In your `logback.xml`:

        <appender name="ELASTIC" class="ElasticsearchAppender">
            <url>http://yourserver/_bulk</url>
            <index>logs-%date{yyyy-MM-dd}</index>
            <type>tester</type>
            <loggerName>es-logger</loggerName> <!-- optional -->
            <errorLoggerName>es-error-logger</errorLoggerName> <!-- optional -->
            <failedEventsLoggerName>es-failed-events</failedEventsLoggerName> <!-- optional -->
            <connectTimeout>30000</connectTimeout> <!-- optional (in ms, default 30000) -->
            <errorsToStderr>false</errorsToStderr> <!-- optional (default false) -->
            <includeCallerData>false</includeCallerData> <!-- optional (default false) -->
            <logsToStderr>false</logsToStderr> <!-- optional (default false) -->
            <maxQueueSize>104857600</maxQueueSize> <!-- optional (default 52428800) -->
            <maxRetries>3</maxRetries> <!-- optional (default 3) -->
            <maxEvents>100</maxEvents><!-- optional (default 1000000) -->
            <readTimeout>30000</readTimeout> <!-- optional (in ms, default 30000) -->
            <sleepTime>250</sleepTime> <!-- optional (in ms, default 250) -->
            <sleepTimeAfterError>15000</sleepTimeAfterError> <!-- optional (in ms, default 15000) -->
            <rawJsonMessage>false</rawJsonMessage> <!-- optional (default false) -->
            <includeMdc>false</includeMdc> <!-- optional (default false) -->
            <excludedMdcKeys>stacktrace</excludedMdcKeys> <!-- optional (default empty) -->
            <maxMessageSize>100</maxMessageSize> <!-- optional (default -1 -->
            <authentication class="BasicAuthentication" /> <!-- optional -->
            <enableContextMap>false</enableContextMap><!-- optional (default false) -->
            <properties>
                <esProperty>
                    <name>host</name>
                    <value>${HOSTNAME}</value>
                    <allowEmpty>false</allowEmpty>
                </esProperty>
                <esProperty>
                    <name>severity</name>
                    <value>%level</value>
                </esProperty>
                <esProperty>
                    <name>thread</name>
                    <value>%thread</value>
                </esProperty>
                <esProperty>
                    <name>stacktrace</name>
                    <value>%ex</value>
                    <ignoredLoggers>at.logger.Class1,at.logger.Class2,my-custom-logger-name</ignoredLoggers>
                </esProperty>
                <esProperty>
                    <name>logger</name>
                    <value>%logger</value>
                </esProperty>
            </properties>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/x-ndjson</value>
                </header>
            </headers>
        </appender>

        <root level="info">
            <appender-ref ref="FILELOGGER" />
            <appender-ref ref="ELASTIC" />
        </root>

        <logger name="es-error-logger" level="INFO" additivity="false">
            <appender-ref ref="FILELOGGER" />
        </logger>

        <logger name="es-failed-events" level="INFO" additivity="false">
            <appender-ref ref="FILELOGGER" />
        </logger>

        <logger name="es-logger" level="INFO" additivity="false">
            <appender name="ES_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                <!-- ... -->
                <encoder>
                    <pattern>%msg</pattern> <!-- This pattern is important, otherwise it won't be the raw Elasticsearch format anyomre -->
                </encoder>
            </appender>
        </logger>



Configuration Reference
=======================

 * `url` (required): The URL to your Elasticsearch bulk API endpoint
 * `index` (required): Name if the index to publish to (populated using PatternLayout just like individual properties - see below)
 * `type` (optional): Elasticsearch `_type` field for records. Although this library does not require `type` to be populated, Elasticsearch may, unless the configured URL includes the type (i.e. `{index}/{type}/_bulk` as opposed to `/_bulk` and `/{index}/_bulk`). See the Elasticsearch [Bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html) documentation for more information
 * `sleepTime` (optional, default 250): Time (in ms) to sleep between attempts at delivering a message
 * `sleepTimeAfterError` (optional, default 15000): Time (in ms) to sleep between failed attempts at delivering a message
 * `maxRetries` (optional, default 3): Number of times to attempt retrying a message on failure. Note that subsequent log messages reset the retry count to 0. This value is important if your program is about to exit (i.e. it is not producing any more log lines) but is unable to deliver some messages to ES
 * `connectTimeout` (optional, default 30000): Elasticsearch connect timeout (in ms)
 * `readTimeout` (optional, default 30000): Elasticsearch read timeout (in ms)
 * `includeCallerData` (optional, default false): If set to `true`, save the caller data (identical to the [AsyncAppender's includeCallerData](http://logback.qos.ch/manual/appenders.html#asyncIncludeCallerData))
 * `errorsToStderr` (optional, default false): If set to `true`, any errors in communicating with Elasticsearch will also be dumped to stderr (normally they are only reported to the internal Logback Status system, in order to prevent a feedback loop)
 * `logsToStderr` (optional, default false): If set to `true`, dump the raw Elasticsearch messages to stderr
 * `maxQueueSize` (optional, default 52,428,800 = 50MB): Maximum size (in characters) of the send buffer. After this point, *logs will be dropped*. This should only happen if Elasticsearch is down, but this is a self-protection mechanism to ensure that the logging system doesn't cause the main process to run out of memory. Note that this maximum is approximate; once the maximum is hit, no new logs will be accepted until it shrinks, but any logs already accepted to be processed will still be added to the buffer
 * `maxEvents` (optional, default 1000000) Maximum amount of logging events to be stored for later sending, set -1 to have unlimited but note that during issues this means you could completely fill the heap with events
 * `loggerName` (optional): If set, raw ES-formatted log data will be sent to this logger
 * `errorLoggerName` (optional): If set, any internal errors or problems will be logged to this logger
 * `failedEventsLoggerName` (optional): If set, any failed event will be logged to this logger
 * `rawJsonMessage` (optional, default false): If set to `true`, the log message is interpreted as pre-formatted raw JSON message.
 * `includeMdc` (optional, default false): If set to `true`, then all [MDC](http://www.slf4j.org/api/org/slf4j/MDC.html) values will be mapped to properties on the JSON payload.
 * `excludedMdcKeys` (optional, default empty): comma separated (extra whitespace is fine) list of case sensitive MDC keys that should not be mapped automatically to properties; only useful when includeMdc is set to `true`
 * `maxMessageSize` (optional, default -1): If set to a number greater than 0, truncate messages larger than this length, then append "`..`" to denote that the message was truncated
 * `authentication` (optional): Add the ability to send authentication headers (see below)
 * `enableContextMap` (optional): If the latest parameter in logger call is of type java.util.Map then all content of it will be traversed and written with prefix `context.*`. For event-specific custom fields.

The fields `@timestamp` and `message` are always sent and can not currently be configured. Additional fields can be sent by adding `<esProperty>` elements to the `<properties>` set.

 * `name` (required): Key to be used in the log event
 * `value` (required): Text string to be sent. Internally, the value is populated using a Logback PatternLayout, so all [Conversion Words](http://logback.qos.ch/manual/layouts.html#conversionWord) can be used (in addition to the standard static variable interpolations like `${HOSTNAME}`).
 * `allowEmpty` (optional, default `false`): Normally, if the `value` results in a `null` or empty string, the field will not be sent. If `allowEmpty` is set to `true` then the field will be sent regardless
 * `type` (optional, default `String`): type of the field on the resulting JSON message. Possible values are: `String`, `int`, `float`, `boolean` and `object`. Use `object` if the value is the string representation of a JSON object or array ie. `{"k" : true}`or `[1,2,3,]`.
 * `ignoredLoggers` (optional, default `<empty>`): An optional comma separated list of loggers for which a field should be ignored even if it exists in the event. Useful if some framework logs stacktraces without real value and they are not needed in elastic. E.g. `at.logger.Class1,at.logger.Class2,my-custom-logger-name`

Groovy Configuration
====================

If you configure logback using `logback.groovy`, this can be configured as follows:

      import de.cgoit.logback.elasticsearch.ElasticsearchAppender
      import de.cgoit.logback.elasticsearch.config.BasicAuthentication
      import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties
      import de.cgoit.logback.elasticsearch.config.HttpRequestHeader
      import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders
      import de.cgoit.logback.elasticsearch.config.Property

      appender("ELASTIC", ElasticsearchAppender) {
        url = 'http://localhost:9200/_bulk'
        authentication = new BasicAuthentication("gpro", '${env.ES_PW_GPRO}')
    
        index = 'logs-%date{yyyy-MM-dd}'
        type = 'log'
    
        rawJsonMessage = false
        errorsToStderr = true
        includeMdc = true
    
        def configHeaders = new HttpRequestHeaders()
        configHeaders.addHeader(new HttpRequestHeader(name: 'Content-Type', value: 'application/x-ndjson'))
        headers = configHeaders
    
        def props = new ElasticsearchProperties()
        props.addProperty(new EsProperty('host', "${hostname}", false))
        props.addProperty(new EsProperty('severity', '%level', false))
        props.addProperty(new EsProperty('thread', '%thread', false))
        props.addProperty(new EsProperty('stacktrace', '%ex', true, 'at.logger.Class1,at.logger.Class2,my-custom-logger-name'))
        props.addProperty(new EsProperty('logger', '%logger', false))
        elasticsearchProperties = props
      }

      root(INFO, ["ELASTIC"])

Authentication
==============

Authentication is a pluggable mechanism. You must specify the authentication class on the XML element itself. The currently supported classes are:

* `BasicAuthentication` - Username and password are taken from the URL (i.e. `http://username:password@yourserver/_bulk`). As an alternative you could use the constructor which accepts a username and a password as parameters (if using this method and logback.groovy you could use environment variables like '${env.MY_VERY_SECRET_PASSWORD}').
* `AWSAuthentication` - Authenticate using the AWS SDK, for use with the [Amazon Elasticsearch Service](https://aws.amazon.com/elasticsearch-service/) (note that you will also need to include `com.amazonaws:aws-java-sdk-core` as a dependency)

Logback Access
==============

Included is also an Elasticsearch appender for Logback Access. The configuration is almost identical, with the following two differences:

 * The Appender class name is `ElasticsearchAccessAppender`
 * The `value` for each `property` uses the [Logback Access conversion words](http://logback.qos.ch/manual/layouts.html#logback-access).

Event-specific custom fields
============================
Log line:

     log.info("Service started in {} seconds", duration/1000, Collections.singletonMap("duration", duration));

Result:

    {
      "@timestamp": "2014-06-04T15:26:14.464+02:00",
      "message": "Service started in 12 seconds",
      "duration": 12368,
    }