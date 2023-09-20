# SkillLevel Service

This service is responsible for calculating and storing the skill levels students gain by completing assessments.

## Usage

A docker-compose file is provided which creates containers for both the service, a postgres database, and a dapr
sidecar.
## Environment variables

### Relevant for deployment

| Name                       | Description                        | Value in Dev Environment                            | Value in Prod Environment                                                  |
|----------------------------|------------------------------------|-----------------------------------------------------|----------------------------------------------------------------------------|
| spring.datasource.url      | PostgreSQL database URL            | jdbc:postgresql://localhost:8032/skilllevel_service | jdbc:postgresql://skilllevel-service-db-postgresql:5432/skilllevel-service |
| spring.datasource.username | Database username                  | root                                                | gits                                                                       |
| spring.datasource.password | Database password                  | root                                                | *secret*                                                                   |
| DAPR_HTTP_PORT             | Dapr HTTP Port                     | 8000                                                | 3500                                                                       |
| server.port                | Port on which the application runs | 8001                                                | 8001                                                                       |

### Other properties
| Name                                      | Description                               | Value in Dev Environment                                         | Value in Prod Environment               |
|-------------------------------------------|-------------------------------------------|------------------------------------------------------------------|-----------------------------------------|
| spring.graphql.graphiql.enabled           | Enable GraphiQL web interface for GraphQL | true                                                             | true                                    |
| spring.graphql.graphiql.path              | Path for GraphiQL when enabled            | /graphiql                                                        | /graphiql                               |
| spring.profiles.active                    | Active Spring profile                     | dev                                                              | prod                                    |
| spring.jpa.properties.hibernate.dialect   | Hibernate dialect for PostgreSQL          | org.hibernate.dialect.PostgreSQLDialect**                        | org.hibernate.dialect.PostgreSQLDialect |
| spring.datasource.driver-class-name       | JDBC driver class                         | org.postgresql.Driver                                            | org.postgresql.Driver                   |
| spring.sql.init.mode                      | SQL initialization mode                   | always                                                           | always                                  |
| spring.jpa.show-sql                       | Show SQL queries in logs                  | true                                                             | true                                    |
| spring.sql.init.continue-on-error         | Continue on SQL init error                | true                                                             | true                                    |
| spring.jpa.hibernate.ddl-auto             | Hibernate DDL auto strategy               | create                                                           | update                                  |
| hibernate.create_empty_composites.enabled | Enable empty composite types in Hibernate | true                                                             | true                                    |
| DAPR_GRPC_PORT                            | Dapr gRPC Port                            | -                                                                | 50001                                   |
| logging.level.root                        | Logging level for root logger             | INFO                                                             | -                                       |
| content_service.url                       | URL for content service GraphQL           | http://localhost:3500/v1.0/invoke/content-service/method/graphql | http://app-content:4001/graphql         |

## Dependencies to Other Services
### Events
The events this service publishes/subscribes to are documented on the wiki:
https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/dapr/dapr-topics.html

### GraphQL
This service sends GraphQL queries to the content service to get information about the assessments and users' progress
with them.

**The ENV variable *CONTENT_SERVICE_URL* needs to be set to the URL of the GraphQL endpoint of the content service**

## API description

The GraphQL API is described in the [api.md file](api.md).

The endpoint for the GraphQL API is `/graphql`. The GraphQL Playground is available at `/graphiql`.

## How to run

How to run services locally is described in the [wiki](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).

