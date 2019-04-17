## Running: Kubernetes

This project uses [Lightbend Orchestration for Kubernetes](https://developer.lightbend.com/docs/lightbend-orchestration-kubernetes/latest/) to
simplify deployment of Online Auction to [Kubernetes](https://kubernetes.io/). Follow the steps below to install Online Auction in your
own local Kubernetes environment.

## Setup

You'll need to ensure that the following software is installed:

* [Docker](https://www.docker.com/)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl)
* [Minikube](https://github.com/kubernetes/minikube) v0.25.0 or later (Verify with `minikube version`)
* [Helm](https://github.com/kubernetes/helm)
* [reactive-cli](https://developer.lightbend.com/docs/reactive-platform-tooling/latest/cli-installation.html#install-the-cli) 0.9.0 or later (Verify with `rp version`)

## Choose Deployment Type

Once you've set up your environment, you can deploy online auction to Kubernetes. The steps differ depending on whether you want to deploy in a development or production environment.

During development, it is simpler to deploy all services with a single sbt task. The *Developer Workflow* section describes how to do this. It will build Docker images for each subproject, generate Kubernetes YAML, and deploy them into your Minikube using `kubectl`.

The *Operations Workflow* section describes the steps for deploying in a production environment. You will use sbt to build the images. The [reactive-cli](https://github.com/lightbend/reactive-cli) is then used to generate YAML for deployment to the Kubernetes cluster. Because the production environment is more complex, additional steps are required as described below.


### Developer Workflow

> Note that this is an alternative to the Operations workflow documented below.

The following workflow is well suited for development purposes. It will build Docker images for each subproject, generate Kubernetes
YAML, and deploy them into your Minikube using `kubectl`. It also installs the [Reactive Sandbox](https://github.com/lightbend/reactive-sandbox)
in your Minikube, providing the required dependencies for Kafka, Cassandra, and Elasticsearch.

> Want a fresh Minikube? Run `minikube delete` before the steps below.

```bash
minikube addons enable ingress
minikube start --memory 6000

sbt "deploy minikube"
```

> Open the URL this command prints in the browser

```bash
echo "http://$(minikube ip)"
```

You should now be able to use the application with your browser. Please note that since Minikube uses a self-signed
certificate for TLS, it will result in a browser warning.

### Operations Workflow

> Note that this is an alternative to the Development workflow documented above.

This workflow matches more closely how an application would be deployed in a production environment. In this method,
the images are built by SBT. The [reactive-cli](https://github.com/lightbend/reactive-cli) is then
used to generate YAML for deployment to the Kubernetes cluster. The steps below focus on Minikube, but could be used on
a real Kubernetes cluster given access to a Docker registry and configured `kubectl`.

> Want a fresh Minikube? Run `minikube delete` before the steps below.

##### 1) Start Minikube and setup Docker to point to Minikube's Docker Engine

```bash
minikube addons enable ingress
minikube start --memory 6000
eval $(minikube docker-env)
```

##### 2) Install Reactive Sandbox (optional; skip if you have your own Cassandra, Kafka, and Elasticsearch clusters)

The `reactive-sandbox` includes development-grade (i.e. it will lose your data) installations of Cassandra, 
Elasticsearch, Kafka, and ZooKeeper. It's packaged as a Helm chart for easy installation into your Kubernetes cluster.

The `reactive-sandbox` allows you to easily test your deployment with Cassandra, Kafka, Zookeeper, and Elasticsearch without having to use the production-oriented versions. Because it is a sandbox, it does not persist data across restarts. The sandbox is packaged as a Helm chart for easy installation into your Kubernetes cluster.

```bash
helm init
helm repo add lightbend-helm-charts  https://repo.lightbend.com/helm-charts
helm repo update
```

Verify that Helm is available (this takes a minute or two):

>  The `-w` flag will watch for changes. Use `CTRL-c` to exit.

```bash
kubectl --namespace kube-system get -w deploy/tiller-deploy
```

```
NAME            DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
tiller-deploy   1         1         1            1           3m
```

Install the sandbox.

```bash
helm install lightbend-helm-charts/reactive-sandbox --name reactive-sandbox
```

Verify that it is available (this takes a minute or two):

>  The `-w` flag will watch for changes. Use `CTRL-c` to exit.

```bash
kubectl get -w deploy/reactive-sandbox
```

```
NAME               DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
reactive-sandbox   1         1         1            1           1m
```

##### 3) Build Project

To build all of the subprojects, run the following command:

```bash
sbt clean docker:publishLocal
```

To build a single subproject, prefix the `docker:publishLocal` task with the project's name and a forward slash. For instance,
the following will only build `itemImpl`:

```bash
sbt itemImpl/docker:publishLocal
```

##### 4) View Images

```bash
docker images
```

##### 5) Deploy Projects

Finally, you're ready to deploy the services. Be sure to adjust the secret variables and cassanda service address as necessary.

```bash
#
# NOTE: You must change the secret values below or the applications will crash.
#
# These values are used for Play's application secret. It is important that they are set to a secret value.
# More information: https://www.playframework.com/documentation/latest/ApplicationSecret

secret_bidding=changeme
secret_item=changeme
secret_user=changeme
secret_search=changeme
secret_web=changeme

# Configure Play's Allowed Hosts filter.
# More information: https://www.playframework.com/documentation/latest/AllowedHostsFilter

allowed_host="$(minikube ip)"

# Default addresses for reactive-sandbox, which provides Cassandra, Kafka, Elasticsearch

export service_cassandra=_cql._tcp.reactive-sandbox-cassandra.default.svc.cluster.local
export service_kafka=_broker._tcp.reactive-sandbox-kafka.default.svc.cluster.local
export service_elasticsearch=_http._tcp.reactive-sandbox-elasticsearch.default.svc.cluster.local

# Deploy bidding-impl

rp generate-kubernetes-resources biddingimpl:1.0.0-SNAPSHOT \
  --generate-pod-controllers --generate-services \
  --env JAVA_OPTS="-Dplay.http.secret.key=$secret_bidding -Dplay.filters.hosts.allowed.0=$allowed_host" \
  --pod-controller-replicas 2 \
  --external-service "cas_native=$service_cassandra" \
  --external-service "kafka_native=$service_kafka" | kubectl apply -f -

# Deploy item-impl

rp generate-kubernetes-resources itemimpl:1.0.0-SNAPSHOT \
  --generate-pod-controllers --generate-services \
  --env JAVA_OPTS="-Dplay.http.secret.key=$secret_item -Dplay.filters.hosts.allowed.0=$allowed_host" \
  --pod-controller-replicas 2 \
  --external-service "cas_native=$service_cassandra" \
  --external-service "kafka_native=$service_kafka" | kubectl apply -f -

# Deploy user-impl

rp generate-kubernetes-resources userimpl:1.0.0-SNAPSHOT \
  --generate-pod-controllers --generate-services \
  --env JAVA_OPTS="-Dplay.http.secret.key=$secret_user -Dplay.filters.hosts.allowed.0=$allowed_host" \
  --pod-controller-replicas 2 \
  --external-service "cas_native=$service_cassandra" \
  --external-service "kafka_native=$service_kafka" | kubectl apply -f -

# Deploy search-impl

rp generate-kubernetes-resources searchimpl:1.0.0-SNAPSHOT \
  --generate-pod-controllers --generate-services \
  --env JAVA_OPTS="-Dplay.http.secret.key=$secret_search -Dplay.filters.hosts.allowed.0=$allowed_host" \
  --pod-controller-replicas 2 \
  --external-service "cas_native=$service_cassandra" \
  --external-service "kafka_native=$service_kafka" \
  --external-service "elastic-search=$service_elasticsearch" | kubectl apply -f -

# Deploy webgateway

rp generate-kubernetes-resources webgateway:1.0.0-SNAPSHOT \
  --generate-pod-controllers --generate-services \
  --env JAVA_OPTS="-Dplay.http.secret.key=$secret_web -Dplay.filters.hosts.allowed.0=$allowed_host" | kubectl apply -f -

# Deploy ingress for everything

# Note that some environments, such as IBM Cloud and Google Kubernetes Engine have slightly different nginx
# implementations. For these, you may need to specify `--ingress-path-suffix '*'` or `--ingress-path-suffix '.*'` as
# part of the command below.

rp generate-kubernetes-resources \
  --generate-ingress --ingress-name online-auction \
  webgateway:1.0.0-SNAPSHOT \
  searchimpl:1.0.0-SNAPSHOT \
  userimpl:1.0.0-SNAPSHOT \
  itemimpl:1.0.0-SNAPSHOT \
  biddingimpl:1.0.0-SNAPSHOT | kubectl apply -f -

```

> Open the URL this command prints in the browser

```bash
echo "http://$(minikube ip)"
```

You should now be able to use the application with your browser. Please note that since Minikube uses a self-signed
certificate for TLS, it will result in a browser warning.
