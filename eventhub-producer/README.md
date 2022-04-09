#EventHubs Producer

This example contains an EventHubs producer, used to produce messages of type 
`com.avpines.spring.messaging.SimpleEvent` either by a given interval or by exposing a REST endpoint
to trigger sending an event manually.

## Produce events by interval

The default behavior of this producer is to send random events at a given interval determined by the
`spring.cloud.stream.poller,fixed-delay` property. For example, the following condifuration will set
the producer to produce a new event every 10ms:

```yaml
spring:
  cloud:
    stream:
      poller:
        fixed-delay: 10
```

To run the produce run the command:

```bash
mvn spring-boot:run
```

## Produce events via REST API

If you wish to have more control about when and what events you are sending, you can use the 
`manual` profile which exposes a REST API, `/send`, for you to send the events.

To run the producer with this profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=manual
```

And to trigger an event, send a `POST` request to the `/send` endpoints, e.g.:
```bash
curl -X POST \
 http://localhost:8080/send \
 -H 'Content-Type: application/json' \
 -d '{ "data": "my-data", "id": "1002" }'
```
