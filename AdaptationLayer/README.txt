QualiMaster Adaptation Layer

This layer is responsible for deciding on the runtime adaptation in a QualiMaster platform. Currently, it is 
just passing through events and communicating with the configuration tool (fixed TCP port 7012), but we will 
integrate the EASy-producer runtime libraries soon. 

Requirements:
- Apache Storm 0.9.3 
- STORM_HOME environment variable set properly

Execution:
Currently, this layer realizes the overall control of the infrastructure. Please check the "scripts" folder
for starting it up and interacting with the infrastructure.
  - main.sh - starts the infrastructure and keeps running. Stop it via CTRL-C. Requires 
    qm.infrastructure.cfg file in the same or your home folder. Please check the scripts directory for an example.
  - cli.sh - a command line client for interacting with the infrastructure. Shows current settings and available 
    commands on execution. Please use logical names from the pipeline, not physical Storm / implementation names.
  - events.sh connects as external client to the infrastructure and shows currently monitored events.

Please note that qualiMaster pipelines must currently be located in a local folder, named according to the pipeline 
name (no version or so) and contain a QualiMaster mapping file. The local folder can be defined in the configuration
files.