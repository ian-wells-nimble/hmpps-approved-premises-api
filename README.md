# Approved Premises API

This is the backend for the Approved Premises service. Its API is consumed by the corresponding "UI" codebase ([approved-premises-ui](https://github.com/ministryofjustice/approved-premises-ui)).

## Prerequisites

TBC

## Setup

When running the application for the first time, run the following command:

```bash
script/setup # TODO - this script is currently a stub
```

If you're coming back to the application after a certain amount of time, you can run:

```bash
script/bootstrap # TODO - this script is currently a stub
```

## Running the application

To run the server, from the root directory, run:

```bash
script/server
```

This runs the project as a Spring Boot application on `localhost:8080`

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/development_database
```

Then in the "Gradle" panel (`View->Tool Windows->Gradle` if not visible), expand `approved-premises-api`, `Tasks`, 
`application` and right click on `bootRunLocal` and select either Run or Debug.

## Making requests to the application

Most endpoints require a JWT from HMPPS Auth - an instance of this runs in Docker locally (started alongside the database) 
on port 9091.  You can get a JWT by running:

```
script/get_client_credentials_jwt
```

The `access_token` value in the output is the JWT.  These are valid for 20 minutes.

This value is then included in the Authorization header on requests to the API, as a bearer token, e.g.

```
Authorization: Bearer {the JWT}
```

## Running the tests

To run linting and tests, from the root directory, run:

```bash
script/test
```

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/test_database
```

Then either:
 - Run or Debug the `verification`, `test` Task from the "Gradle" panel
 - Open an individual test class and click the green play icon in the gutter at the class level or on a specific test method

## OpenAPI documentation

The API which is offered to front-end UI apps is documented using Swagger/OpenAPI.
The initial contract covers the migration of certain bed-management functions from Delius into the new service.

This is available in development at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Infrastructure

The service is deployed to the [MoJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk). This is 
managed by Kubernetes and Helm Charts which reside within this repo at [`./helm_deploy`](./helm_deploy/approved-premises-api/).


To get set up with Kubernetes and configure your system so that the `kubectl` command authenticates, see this 
[[MoJ guide to generating a 'kube' config](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#generating-a-kubeconfig-file)].

You should then be able to run `kubectl` commands, e.g. to list the 'pods' in a given 'namespace':

```bash
$ kubectl -n hmpps-approved-premises-dev get pods

NAME                                           READY   STATUS    RESTARTS   AGE
hmpps-approved-premises-api-6958c57d9f-plgpm   1/1     Running   0          86m
hmpps-approved-premises-api-6958c57d9f-sgxkc   1/1     Running   0          86m
hmpps-approved-premises-ui-6c5b76c477-kmljb    1/1     Running   0          38m
hmpps-approved-premises-ui-6c5b76c477-t62b4    1/1     Running   0          38m
```
**NB**: this [`kubectl` cheatsheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/) is a good reference to 
other commands you may need.