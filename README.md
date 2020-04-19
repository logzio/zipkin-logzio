# Ship Zipkin traces

Zipkin-Logz.io Trace Storage is a storage option for Zipkin distributed traces on your Logz.io account.
It functions as both a collector and a span store.

**Note**:
  This integration requires Logz.io API access.
  The Logz.io API is available for all Enterprise accounts.
  If you're on a Pro account, reach out to your account manager or the <a class="intercom-launch" href="mailto:sales@logz.io">Sales team</a> to request API access.

### Limitations

When you use the Zipkin UI to find traces stored in Logz.io, there are a couple limitations.
For most users, these won't be an issue, but they're still good to know:

* **Lookback** must be 2 days or less
* **Limit** must be 1000 traces or less

## To integrate Zipkin server and Logz.io

You can either [run as a docker](#run-as-a-docker) or run as a [java app](#1-download-zipkin-server-and-zipkin-logzio-trace-storage).

### Run as a docker

If you don't have docker, install it first - instructions [here](http://docs.docker.com/engine/installation/)

Run the docker with the the appropriate [environment variables](#parameters) (no need to set STORAGE_TYPE env).
```bash
docker run -d -p 9411:9411 -e LOGZIO_ACCOUNT_TOKEN=<ACCOUNT-TOKEN> -e LOGZIO_API_TOKEN=<API-TOKEN> logzio/zipkin 
```


### 1. Download Zipkin server and Zipkin-Logz.io Trace Storage

Download [Zipkin server](https://search.maven.org/remote_content?g=io.zipkin&a=zipkin-server&v=LATEST&c=exec).

```bash
curl -sSL https://zipkin.io/quickstart.sh | bash -s
```

Download the latest [Zipkin-Logz.io Trace Storage](https://jitpack.io/#logzio/zipkin-logzio) jar to the same directory.
```bash
curl -sSL https://jitpack.io/com/github/logzio/zipkin-logzio/zipkin-module-storage-logzio/master-SNAPSHOT/zipkin-module-storage-logzio-master-SNAPSHOT-module.jar > logzio.jar
```

### 2. Run Zipkin server with the Logz.io extension

You can configure the Logz.io extension with shell variables or environment variables.

For a complete list of options, see the parameters below the code block.👇

```bash
STORAGE_TYPE=logzio \
LOGZIO_ACCOUNT_TOKEN=<ACCOUNT-TOKEN> \
LOGZIO_LISTENER_HOST=<LISTENER-URL> \
LOGZIO_API_TOKEN=<API-TOKEN> \
LOGZIO_API_HOST=<API-URL> \
java -Dloader.path='logzio.jar,logzio.jar!lib' -Dspring.profiles.active=logzio -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

**Pro tip**:
You can optionally run two discrete Zipkin-Logzio Trace Storage instances if you want to separate shipping and reading of your traces.
If you do, then the required fields change a bit from what's shown in the Parameters list:

* The **shipping instance** uses `STORAGE_TYPE=logzio`, `LOGZIO_ACCOUNT_TOKEN`, and `LOGZIO_LISTENER_HOST`.
* The **reading instance** uses `STORAGE_TYPE=logzio`, `LOGZIO_API_TOKEN`, and `LOGZIO_API_HOST`.

#### Parameters

| Parameter | Description |
|---|---|
| **STORAGE_TYPE=logzio** | **Required**. <br> We wish there was a way to include this as a default. Alas, Zipkin needs it, so you'll need to include this bit. |
| **LOGZIO_ACCOUNT_TOKEN** | **Required**. <br> Required when using as a collector to ship logs to Logz.io. <br> Replace `<ACCOUNT-TOKEN>` with the [token](https://app.logz.io/#/dashboard/settings/general) of the account you want to ship to. |
| **LOGZIO_API_TOKEN** | **Required**. <br> Required to read back traces from Logz.io. <br> Replace `<API-TOKEN>` with an [API token](https://app.logz.io/#/dashboard/settings/api-tokens) from the account you want to use. |
| **LOGZIO_LISTENER_HOST** | **Default**: `listener.logz.io` <br> Replace `<LISTENER-URL>` with your region's listener URL. For more information on finding your account's region, see [Account region](https://docs.logz.io/user-guide/accounts/account-region.html). |
| **LOGZIO_API_HOST** | **Default**: `api.logz.io` <br> Required to read back spans from Logz.io. <br> Replace `<API-URL>` with your region's base API URL. For more information on finding your account's region, see [Account region](https://docs.logz.io/user-guide/accounts/account-region.html). |
| **STRICT_TRACE_ID** | **Default**: `true` <br> Use `false` if your version of Zipkin server generates 64-bit trace IDs (version 1.14 or lower). If `false`, spans are grouped by the rightmost 16 characters of the trace ID. For version 1.15 or later, we recommend leaving the default. |
| **SENDER_DRAIN_INTERVAL** |  **Default**: `5` <br> Time interval, in seconds, to send the traces accumulated on the disk. |
| **CLEAN_SENT_TRACES_INTERVAL** | **Default**: `30` <br> Time interval, in seconds, to clean sent traces from the disk. |

### 3. Check Logz.io for your traces

Give your traces some time to get from your system to ours, and then open [Kibana](https://app.logz.io/#/dashboard/kibana).

If you still don't see your logs, see [log shipping troubleshooting](https://docs.logz.io/user-guide/log-shipping/log-shipping-troubleshooting.html).


### Changelog
- v0.0.4
  * Provide a docker for the integration
  * Added some tests
  * Updates to Zipkin 2.16 by vendoring internal code previously borrowed
- v0.0.3
  * Update vulnerable dependencies 
- v0.0.2
  * Use better Zipkin libraries
  * Much lighter jar file
