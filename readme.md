# Word count

Counts the words in sliding window grouped by event_type as they come from stdin.

Example for event
```json
{ "event_type": "baz", "data": "lorem", "timestamp": 1604082980 }
```

Run as
```
./producer | sbt run
```

And open [http://localhost:8080/](http://localhost:8080/)