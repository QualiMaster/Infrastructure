QualiMaster EventBus

This layer is responsible for distributing events among the the infrastructure layers and for forward
definitions of global events. Events are used for passing information from lower to higher layers
to the infrastructure (direct calls in this direction are forbidden), while direct calls in the 
opposite direction are allowed.

We may replace the mechanism behind by Apache Kafka or a JMS depending on future experience.