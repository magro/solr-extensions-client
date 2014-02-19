A new loadbalanced, failover capable SolrJ client
======================

The `solr-extensions-client` is a new implementation of the [`LBHttpSolrServer`](http://wiki.apache.org/solr/LBHttpSolrServer) shipping with [Solr](http://lucene.apache.org/solr). It is designed to be open for extension and enables a transparent request failover.

The `LBHttpSolrServer` does a great job when it comes to balance requests across a bunch of Solr servers. But unfortunately it is not designed to be open for extension, which this implementation tries to solve.

The `FailoverLoadbalancedHttpSolrServer` extends the `SolrServer` class and exposes the following capabilities

* Load balancing strategy
* Exception handling strategy
* Instance healthchecks

## Use

    # create a new client
    FailoverLoadbalancedHttpSolrServer server = new FailoverLoadbalancedHttpSolrServer(
       httpClient,
       new BinaryResponseParser(), 
       new RoundRobinSolrLoadBalancerStrategy(), 
       new ByErrorRateSolrExceptionHandler(),
       new PingAwareSolrServerAliveChecker(5);
       
    # start adding solr urls
    server.addSolrServer("http://some.solr.url/solr");

### Configuration

The project contains some utility classes:

* `ExplanatoryHttpSolrServer` – does a proper to string for the `HttpSolrServer`
* `HttpClientSolrConfigurationBean` – configures a http client with some Solr defaults. Use in IoC containers like Spring.

#### `SolrLoadBalancingStrategy`

Default load balancing strategy is the `RoundRobinSolrLoadBalancerStrategy` that distributes requests round robin across the configured servers. Dead servers are skipped and a exception is thrown if no alive Solr server is left.

When issueing more than one request to a Solr server, it might be handy to send those to the same Solr instance. If you for example request available facets for a given query in a first request and retrieve matching documents in a subsequent request, query time will decrease when working on the same facet/filter/document caches.

To achieve this, the `StickyDelegatingSolrLoadBalancerStrategy` utilizes the Spring `RequestContextHolder` to store the first Solr instance used in the current request context. The following requests are sent to the same Solr instance.

#### `SolrServerAliveChecker`

`PingAwareSolrServerAliveChecker` – iterates through all configured servers and 
checks their availability via a ping request

`DeadSolrServerAliveChecker` – iterates through all servers marked as dead and checks their re-availability via a ping request

#### `SolrExceptionHandler`

`ByErrorRateSolrExceptionHandler` – disables a Solr instance if the thrown exception rate exceeds the configured error rate per minute

## Extend

Contributions are welcome! We'd like to see async http client support and more sophisticated lb strategies.

## Build

You need to haven Maven installed. Check out the project, run

    mvn clean verify
    
You'll find the client `jar` in the `target` directory.

## Release

In order to do a release we have to prepare the release

    $ mvn release:prepare
    
The parameter `developmentVersion` can be used to set the new version of your local working copy. Afterwards we perform the release

    $ mvn release:perform

## License

[Apache License](LICENSE)