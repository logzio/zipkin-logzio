# zipkin-logz.io
Zipkin logz.io is a storage option for Zipkin traces on a logz.io account. **The Storage extension is available for logz.io users with a PRO account or above** 
It can function as a Collector and/or as a Span store.

## Server integration
In order to integrate with zipkin-server, you need to use properties
launcher to load the storage extension alongside the zipkin-server
process.

To integrate the extension with a Zipkin server, you need to:
* Download the extension jar to the directory containing the zipkin-server 
* enable the logzio profile
* launch Zipkin with `PropertiesLauncher`

## Example integrating the logz.io storage extension

Here's an example of integrating the logz.io extension.

### Step 1: Download zipkin-server jar
Download the [latest released server](https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec) as zipkin.jar:

```
wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
```

### Step 2: Download the latest zipkin-storage-logzio jar from the release

### Step 3 (Optional): create an API-TOKEN that will be associated with zipkin-logzio 
Go to [Logz.io API TOKENS settings page](https://app.logz.io/#/dashboard/settings/api-tokens) and create an API TOKEN.

### Step 4: Run the server with the "logzio" profile active
When you enable the "logzio" profile, you can configure logz.io extension with
short environment variables.

``` bash
LOGZIO_LISTENER_ADDR=https://listener.logz.io:8071 \
API_TOKEN=<API_TOKEN> \
LOGZIO_TOKEN=<ACCOUNT_TOKEN> \
STORAGE_TYPE=logzio \
java -Dloader.path='zipkin-logzio.jar,zipkin-logzio.jar!lib' -Dspring.profiles.active=logzio -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher

```
* **NOTE:** Make sure the parameters are defined in the same line or use environment variables **

* Configures
  * `STORAGE_TYPE=logzio` : **required**. 
  * `LOGZIO_TOKEN` : **required when using as a Collector** Your logz.io [account token](https://app.logz.io/#/dashboard/settings/manage-accounts). 
  * `API_TOKEN` : **required when using to read back spans from logz.io** Your API TOKEN that you created at step 3.
  * `LOGZIO_LISTENER_ADDR` (default: https://listener.logz.io:8071) : logz.io Listener address.
  * `STRICT_TRACE_ID` (default: true) If strict trace ID is set to false, spans are grouped by the right-most 16 characters of the trace ID.

### Limitation

 When searching for traces, the time range can't be more than 2 days long.
 The number of traces you can search for is limited to 1000.
 
### Contributing

We welcome any contribution! Here's how you can help:

  - Fork this repo
  - Open an issue (Bug, Feature request, etc)
  - Create a PR for the additional functionality
  - Make sure all the tests pass
