# MetaDiscoveryThesis

How to use:

TODO:

There are two options to use the Meta Discovery Algorithm.
- You can either copy the .env and docker-compose.yml file and 
use the docker compose up -d command to start the containers. This will pull the last build images from the docker repo,
if you want, you can clone the project and build it yourself using
- ant buildHudson followed by ant build-docker
Then you need to go in the ./docker folder and again use:
- docker compose up -d

The .env file contains some configuration that needs to be adapted to your needs.
After you have started the containers you can use
- optuna-dashboard postgresql+psycopg2://postgres:postgres@localhost:5432

To start the optuna container and see the progress of the pipeline.



