# Theia Container Controller

Right now uses cookies to check for username

To connect to a users theia-ide send get to [address]/theia

The controller will start a container instance and redirect to it (Or just redirect if an instance is already running)

To run using local build use docker-compose up

To run using container registry navigate to production folder then use docker-compose up