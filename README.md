# SkillLevel Service

This service is responsible for calculating and storing the skill levels students gain by completing assessments.

# Usage

A docker-compose file is provided which creates containers for both the service, a postgres database, and a dapr
sidecar.

# Dependencies to Other Services
## Events
The events this service publishes/subscribes to are documented on the wiki:
https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/dapr/dapr-topics.html

## GraphQL
This service sends GraphQL queries to the content service to get information about the assessments and users' progress
with them.

**The ENV variable *CONTENT_SERVICE_URL* needs to be set to the URL of the GraphQL endpoint of the content service**