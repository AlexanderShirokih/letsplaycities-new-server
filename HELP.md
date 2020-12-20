# Let's Play Cities Game Server

### Building

You can get docker image using `spring-boot:build-image` target

### Creating container from image

To create container use the following snippet:

```
docker run --network="host" -v ~/Workspaces/workspace-spring-tool-suite/LPSServer/data:/workspace/data --name lps_server lpsserver:1.4.6
```

Replace `~/Workspaces/workspace-spring-tool-suite/LPSServer/data` with host machine path containing real program data

### Program data

- `data/env.config.properties` - server config properties (Search it in the source code)
- `data/database/login.ban` - line separated list of banned logins. First line it a special header that should contain
  items count.
- `data/database/data-${version}.db2` - database files.

### Setting up MySQL server

You should set up MySQL and create scheme by itself. There are no instructions.

### Setting up passwords

You should insert `config.yml` file containing database connection credentials and keystore password.
See `application.yml` for help.
