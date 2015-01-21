import Direction._

/** 
  * To decide which request is the most important we prioritize longer 
  * waiting requests with respect to the distance to the nearest elevator
  * and taking into account existing elevator queue workload and distance 
  * to the pickup floor for non-idle elevators.
  *
  * Distance to the nearest elevator is calculated as workload distance and
  * pickup distance, where:
  * - workload distance is difference between current floor and final goal floor
  * - pickup distance is distance between final goal floor and the floor where
  *   request originated from.
  *
  * Final weight is calculated as (2t - (wd + pd) + M) where
  *
  * t = time difference since request originated
  * wd = abs(current - goal)
  * pd = abs(pickup floor - goal)
 *  M = Number of floors
  *
  * This ensures that while near ones can get picked up quickly,
  * every request should be processed as it is only ever
  * gaining in priority.
 *
 * DropOffRequest represents immutable data structure representing single drop off request
 * issued by a person in an elevator.
 *
 * @param floor      Floor where request originated from
 * @param direction  Direction where person wants to go
 *
 * PickupRequest represents immutable data structure representing single pickup request
 * with auxiliary methods for calculations.
 *
 * @param floor      Floor where request originated from
 * @param direction  Direction where person wants to go
 * @param created    Timestamp of when the request was initiated
  */

trait Request {
  val floor: Int
  val direction: Direction
}
case class DropOffRequest(floor: Int, direction: Direction) extends Request
case class PickUpRequest(floor: Int, direction: Direction, created: Int) extends Request {
  def nearestElevators(elevators: List[Elevator]): List[Elevator] = elevators.sortBy(elv => elv.totalCost(this))
  def nearestElevator(elevators: List[Elevator]): Elevator = nearestElevators(elevators).head

  /**
   * Weight function is designed around the following objectives:
   *  - We want to give priority to the nearest elevators
   *  - However, we don't want far away elevators to wait too long, so we give
   *    higher priority to requests that have been waiting for longer
   * 
   * An equation that matches these requirements is w = 2 t - (-d + M),
   * where d is distance to the nearest elevator, M is the number of floors
   * and t is time difference.
   * 
   * Function profile for 10 story building: http://www.wolframalpha.com/input/?i=Plot3D%5B2t+-+d+%2B+10%2C+%7Bd%2C+0%2C+15%7D%2C+%7Bt%2C+0%2C+15%7D%5D
   * 
   * @param ctime Current time
   * @param elevator (Nearest) elevator for which we are calculating the weight from
   * @return Weight
   */
//  def weight(ctime: Int, elevator: Elevator): Double = math.pow(math.abs(ctime - created).toDouble, 1.3) / elevator.totalCost(this).toDouble
  def weight(ctime: Int, elevator: Elevator, numFloors: Int): Double = 2 * math.abs(ctime - created).toDouble - elevator.totalCost(this).toDouble + numFloors
}

/** Elevator represents immutable data structure for single elevator with
  * current floor, goal flor and queue being mutable attributes.
 * 
 * @param id       ID of the elevator
 * @param current  Current floor where elevator is located at
 * @param goal     Final floor where elevator is going
 * @param queue    Queue of floors where we have to stop either to pick up people or to drop them off
 */
class Elevator(val id: Int, var current: Int, var goal: Int, var queue: List[Request] = List()) {
  /**
   * isBusy/isIdle functions
   * @return Boolean whether elevator is busy or idling.
   */
  def isBusy: Boolean = current != goal && queue.nonEmpty
  def isIdle: Boolean = current == goal && queue.isEmpty
  
  /**
   * Enqueue pickup request to the list
   * * */
  def enqueueRequest(request: Request) = {
    queue = request :: queue
  }

  def goToFloor(dfloor: Int) = {
    def getDirection(dfloor: Int): Direction = {
      val dif = dfloor - current
      if (dif > 0) Up
      else if (dif < 0) Down
      else Idle    }
    enqueueRequest(DropOffRequest(dfloor, getDirection(dfloor)))
  }
  
  /**
   * Return distance to empty the queue and until getting to the request floor.
   *
   * @return  Return distance
   */
  def workloadCost: Int = math.abs(goal - current)
  def totalCost(req: PickUpRequest): Int = {
    if (canPickup(req)) math.abs(req.floor - current)
    else workloadCost + math.abs(goal - req.floor)
  }

  /**
   * Returns an integer represeting step in a single direction 
   * @return 1 for moving up one floor, -1 for moving down, 0 for idling
   */
  def getStep: Int = {
    val gc = goal - current
    if (gc > 0) 1
    else if (gc < 0) -1
    else 0
  }

  /**
   * Return target direction, that is which direction are people from this
   * elevator going to and not which direction elevator is actually traveling.
   * 
   * @return  Target direction
   */
  def getTargetDirection: Direction = {
    if (queue.nonEmpty) queue.head.direction
    else Idle
  }

  /**
   * Return actual traveling direction of the elevator at that moment
   * @return  Direction
   */
  def getDirection: Direction = {
    val gs = getStep
    if (gs > 0) Up
    else if (gs < 0) Down
    else Idle
  }

  /**
   * Returns whether a particular elevator going into current direction can serve pickup request.
   * 
   * Verify that the request is in a specified range ([] denoting mathematical inclusion of range):
   * - Up: [current floor, max(goal, request floor)]
   * - Down: [min(goal, request floor), current floor]
   * 
   * AND that elevator is currently going into that direction AND that it's target direction is the same.
   * 
   * @param request PickUpRequest object representing request
   * @return  Boolean Can we serve the request?
   */
  def canPickup(request: PickUpRequest): Boolean = {
    def onRoute(request: PickUpRequest): Boolean = {
      if (request.direction == Up) current < request.floor && request.floor <= math.max(request.floor, goal)
      else if (request.direction == Down) math.min(request.floor, goal) <= request.floor && request.floor < current
      else false
    }
    request.direction == getTargetDirection && request.direction == getDirection && onRoute(request)
  }
  
  /*
  Start step operation - if anything is in our queue, (re)adjust goal if needed
   */
  def startStep() = {
    val floors = queue.map(pr => pr.floor)
    val direction = getDirection
    if (direction == Up) goal = floors.max
    else if (direction == Down) goal = floors.min
    else if (floors.nonEmpty) goal = floors.head
  }

  /*
   Move to new floor
   */
  def doStep() = {
    current = current + getStep
  }

  /**
   * "Pickup people that are going in the same direction"; remove request from queue
   */
  def endStep() = {
    queue = queue.filter(_.floor != current)
  }
}

/**
 * ElevatorControlSystem
 * 
 * Master control system for elevators.
 * 
 * One step represents in my simplified model as one time unit 
 * a single move operation (moving one floor up/down) and
 * serving requests on that floor (picking up or dropping of people).
  
  Start
  |                  Move                     |
  |------------------------------------------>|
  |                                           |
                                          Stop
 *
 * On START ElevatorControlSystem distributes the tasks into elevator queues
 * with the following logic:
 * - If any elevator is idling nearer than anything on route, select idle elevator
 * - If there is any elevator already on route and going into the 
 *   same direction as the request, pick nearest on route elevator
 * - If there are any idle elevators available assign remaining requests to the idle elevator(s)
 *   based on weighting rules specified in PickUpRequest
 *   
 * On STOP Elevators clear their queue if they reached a floor where request has been issued from ("open the door").
 * 
 * @param numElevators  Number of elevators that our system is to operate
 * @param numFloors     Number of floors that are in the building
 */
class ElevatorControlSystem(val numElevators: Int, val numFloors: Int) {
  private var stepNum = 0
  def getStep: Int = stepNum
  
  var pickupQueue: List[PickUpRequest] = List()
  def dequeueRequest(request: PickUpRequest) = pickupQueue = pickupQueue.filter(_ != request)
  
  val elevators: List[Elevator] = (1 to numElevators).map(x => new Elevator(x, 0, 0)).toList

  def idleElevators: Seq[Elevator] = elevators.filter(_.isIdle)

  def getElevator(id: Int): Elevator = elevators(id)
  
  def status: Seq[(Int, Int, Int)] = elevators.map(elv => (elv.id, elv.current, elv.goal))
  def pickup(pfloor: Int, direction: Int) = pickupQueue = PickUpRequest(pfloor, intToDirection(direction), stepNum) :: pickupQueue
  
  def step() = {
    stepNum = stepNum + 1
    
    // Prioritize requests based on their weight, using time and nearestElevator metrics
    pickupQueue = pickupQueue.sortBy(pr => -pr.weight(stepNum, pr.nearestElevator(elevators), numFloors))

    // If pickup is closer than anything idling, let's pick it up, otherwise use idle elevator to increase liquidity
    // and distribute load among more elevators.
    pickupQueue.foreach{req =>
      elevators.filter(!_.isIdle).filter(_.canPickup(req)).sortBy(_.totalCost(req)).headOption match {
        case Some(elvOnRoute) =>
          idleElevators.sortBy(_.totalCost(req)).headOption match {
            case Some(idleElv) => if (idleElv.totalCost(req) < elvOnRoute.totalCost(req)) {
              idleElv.enqueueRequest(req)
              dequeueRequest(req)
            } else {
              elvOnRoute.enqueueRequest(req)
              dequeueRequest(req)
            }
            case None => {
              elvOnRoute.enqueueRequest(req)
              dequeueRequest(req)
            }
          }
        case None =>
      }
    }
    
    // If we have idle elevators, redirect those to request position
    while (pickupQueue.nonEmpty && idleElevators.nonEmpty) {
      val req = pickupQueue.head
      pickupQueue = pickupQueue.tail
      val idles = idleElevators.sortBy(_.totalCost(req))
      idles.head.enqueueRequest(req)
    }
    elevators.map(_.startStep())
    
    elevators.map(_.doStep())
    
    elevators.map(_.endStep())
  }
}