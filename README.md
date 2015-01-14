# Elevator challenge
A short (4 hour time limit) elevator challenge for a distributed applications engineer position at a certain startup. Instructions pretty loose: implement interface for elevator system where system has to support certain operations. Per their request I’m not publishing the detailed instructions.

Specified: 50% working solution better than 99% non-working solution.

## Build instructions

Requirements: Scala, SBT.

Running tests: `sbt test`.

CLI interface is not provided in a current version.

## Elevator Control System - discussion

Considering time constraints and spec I decided to give it a go with a simple FIFO queue with one person per elevator with load balancing based on how far are idle elevators are from the floor where pickup request originated.

All pickup requests go to a master queue (`pickupQueue`) which in my dumb implementation is just a simple FIFO queue. A lot better (but outside 4 hour version) queue would be a something along the lines of `priorityQueue`, probably best weighted by something like `time in queue / distance to pickup floor`. This would mean that queue would have to be recomputed on every time tick.

Another thing that is missing is picking up people on the way if they go in the same direction.

