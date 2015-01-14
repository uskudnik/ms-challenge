//import scala.collection.mutable.PriorityQueue

import scala.collection.mutable

class PickUpQueue {
  type PickupRequest = (Int, Int)

  def nonEmpty: Boolean = queue.nonEmpty
  def isEmpty: Boolean = queue.isEmpty
  def length: Int = queue.length
  def toList: List[PickupRequest] = queue.toList


  private val queue = new mutable.Queue[PickupRequest]()

  def enqueue(pickup: PickupRequest) = queue.enqueue(pickup)
  def dequeue() = queue.dequeue()
}

class ElevatorQueue {
  def nonEmpty: Boolean = queue.nonEmpty
  def isEmpty: Boolean = queue.isEmpty
  def length: Int = queue.length

  type Floor = Int
  
  private val queue = new mutable.Queue[Floor]()
  
  def enqueue(to: Floor) = queue.enqueue(to)
  def dequeue = queue.dequeue()


  def cost: Int = queue.reduce(_ + _) // TODO not correct calculation just yet
}

object Direction extends Enumeration {
  type Direction = Value
  val Up, Down, Stop = Value
}
import Direction._

case class Elevator(id: Int, var current: Int, var goal: Int, queue: ElevatorQueue) {
  def getStep: Int = {
    val gc = goal - current
    if (gc > 0) 1
    else if (gc < 0) -1
    else 0
  }

  def getDirection: Direction = {
    val gs = getStep
    if (gs > 0) Up
    else if (gs < 0) Down
    else Stop
  }
  
//  def toFloor(floor: Int) =

  def isBusy: Boolean = current != goal && queue.nonEmpty
  def isIdle: Boolean = current == goal && queue.isEmpty
  
  def update(goal: Int) = current = goal
  def workloadCost: Int = math.abs(goal - current)
  def pickUpCost: Int => Int = pfloor => workloadCost + math.abs(pfloor - goal) // TODO if queue not empty, take that into account
}

class ElevatorControlSystem(val numElevators: Int) {
  type PickupRequest = (Int, Int)

  private var stepNum = 0
  def getStep: Int = stepNum
  
  val pickupQueue = new PickUpQueue()

  private val elevators: Seq[Elevator] = (1 to numElevators).map(x => Elevator(x, 0, 0, new ElevatorQueue()))
  def idleElevators: Seq[Elevator] = elevators.filter(_.isIdle)

  def getElevator(id: Int): Elevator = elevators(id)
  
  def status: Seq[(Int, Int, Int)] = elevators.map(elv => (elv.id, elv.current, elv.goal))
  def pickup(pfloor: Int, direction: Int) = pickupQueue.enqueue((pfloor, direction))
  def update(id: Int, current: Int, goal: Int) = {}
  def step() = {
    stepNum = stepNum + 1

    while (idleElevators.nonEmpty && pickupQueue.nonEmpty) {
      val (pfloor, dir) = pickupQueue.dequeue()
      val idles = idleElevators.sortBy(_.pickUpCost(pfloor))
      val elv = idles.head
      println(pickupQueue.length + " queue: " + pickupQueue + "; idles: " + idles + ", top: " + elv)
      elv.goal = pfloor
    }
    
    elevators.map(elv => elv.update(elv.current + elv.getStep))
  }
}