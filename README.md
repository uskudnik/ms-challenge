# Elevator challenge
A short (4 hour time limit) elevator challenge for a distributed applications engineer position at a certain startup. Instructions pretty loose: implement interface for elevator system where system has to support certain operations. Per their request I’m not publishing the detailed instructions.

Specified: 50% working solution better than 99% non-working solution.

## Build instructions

Requirements: Scala, SBT.

Running tests: `sbt test`.

CLI interface is not provided in a current version.

## Elevator Control System - initial (4 hour) version

Considering time constraints and spec I decided to give it a go with a simple FIFO queue with one person per elevator with load balancing based on how far are idle elevators are from the floor where pickup request originated.

All pickup requests go to a master queue (`pickupQueue`) which in my dumb implementation is just a simple FIFO queue. A lot better (but outside 4 hour version) queue would be a something along the lines of `priorityQueue`, probably best weighted by something like `time in queue / distance to pickup floor`. This would mean that queue would have to be recomputed on every time tick.

Another thing that is missing is picking up people on the way if they go in the same direction.

## Elevator Control System - notes on FIFO version

While my initial version was based on FIFO principle and with single elevator for every request (and 1 person per elevator) and I considered it very inefficient it turns out that it’s not actually _that bad_ - in fact it appears [top-of-the-line elevator systems](http://www.npr.org/templates/story/story.php?storyId=122457774) optimise with exactly such FIFO principle:

```
The new Baltimore headquarters of the asset management firm Legg Mason has a similar elevator system. In the lobby, employees scan in their employee ID cards at a turnstile, and an LCD screen flashes which elevator to take. The system already knows where people are going based on their ID cards and generally by the time employees arrive at the elevators, one is waiting.The elevator stops at only one floor.

In many Destination Dispatch elevators, there are no buttons inside. If you accidentally get into another person's elevator or input the wrong floor, you must wait to exit the elevator to choose another floor.

This tends to freak people out at first, according to a person at the front desk of the AAAS building, the first building to install Destination Dispatch in Washington, D.C.

But the important thing, engineers say, is that the system saves time and energy.

```

That being said, by the time I saw that article I had most of the second version of code implemented anyway - either way, if your building is not “smart” (scanning ID cards on entering the building), FIFO probably wouldn’t be such a good idea anyway.

## Elevator Control System

That being said, my second implementation improves on the initial version on all the ideas I had for the first one but didn’t have time to properly implement (and a bit more) and test - proper weighting - using distance and time parameters, picking up on route and only ever going one way, deciding whether it’s better to wait for incoming transport or if idle elevator is in vicinity use idle one.

In the end I chose not to use a proper `Priority Queue` data structure for my implementation as the priority had to be recomputed on every time tick (and then a new instance of `Priority Queue` built) and just went with list sorted based on weight.

I sort the pickup queue with the following formula:

```
weight = 2 * time - distance + numFloors

weight    = score
time      = time since the request was initiated
distance  = calculated for the elevator with the smallest total cost; calculated from the current workload cost (how many steps before completing current run) + cost from goal to floor where request originated from
numFloors = number of floors in the building
```

While weight is always bound to increase I nonetheless decided to add a bit more weight to the time factor so that far-away (and more time consuming) requests get served a bit quicker (and a cluster of requests from original location will during that time gain weight and will get served in a batch when elevator comes close to them again)

Factors (2 and numFloors) were not analytically chosen - I was thinking about maybe trying to do some kind of Monte Carlo simulation that would try to measure time to serve every request but at the current moment I haven’t come around to that yet. For a production system these factors should probably be determined with a simulation.

The logic for the queue goes like this:
1. If there is an idle elevator in vicinity and is nearer than the elevator that is busy, pick the idle elevator
2. If there is an existing ride going into the same direction as the request AND the request is originating from a floor that is before the goal and on or after the current floor, let pick the elevator riding already.
3. If there are any remaining idle elevators, add ride to them.

Drop-off request can only be initiated from within the elevator, therefore they are only ever present in each elevator’s individual queue.
