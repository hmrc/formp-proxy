
# formp-proxy

This FormP Proxy service connects to the FormP Oracle database.

## Running the service

Service Manager: `sm2 --start FORMP_PROXY`

To start the server locally: `sbt 'run 6995'`

## Testing

Run unit tests with:
```shell
sbt test
```

Check code coverage with:
```shell
sbt clean coverage test it/test coverageReport
```

Run integration tests with:
```shell
sbt it/test
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
