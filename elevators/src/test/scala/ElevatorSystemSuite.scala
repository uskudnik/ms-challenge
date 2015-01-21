import org.junit.runner.RunWith
import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import Direction._

@RunWith(classOf[JUnitRunner])
class ElevatorSystemSuite extends FunSuite {
  trait ElevatorSystemSetUp {
    val elevator = new Elevator(1, 3, 9)
    val elv1 = new ElevatorControlSystem(1, 10)
    val elv2 = new ElevatorControlSystem(2, 10)

    val req1 = PickUpRequest(8, Down, 0)
    val req2 = PickUpRequest(5, Up, 0)
    val req3 = PickUpRequest(10, Down, 0)
    val req4 = PickUpRequest(12, Up, 0)
    
    def step() = {
      elv1.step()
      elv2.step()
    }
    
    def doSteps(num: Int) = {
      (1 to num).map(x => step())
    }
    
    def status() = {
      println("\nStep:    " + elv1.getStep)
      println("---- ---- ---- ----")
      println("Pickup Queue 1:    " + elv1.pickupQueue.toList)
      println("Pickup Queue 2:    " + elv2.pickupQueue.toList)
      println("---- ---- ---- ----")
      println("Elevator system 1:    " + elv1.status.toList)
      println("Elevator system 2:    " + elv2.status.toList)
      println("---- ---- ---- ----")
      println("Elevator Queue 1:    " + elv1.elevators.map(elv => elv.queue))
      println("Elevator Queue 2:    " + elv2.elevators.map(elv => elv.queue))
      println("---- ---- ---- ----")
    }
  }
  
  test("Working status") {
    new ElevatorSystemSetUp {
      assert(elv2.status.toList == List((1,0,0), (2,0,0)), "Status does not appear to operate nominally.")
    }
  }

  test("Simple pickup request") {
    new ElevatorSystemSetUp {
      // Make pickup request
      elv1.pickup(8, -1)
      // Ensure thing queue is full
      assert(elv1.pickupQueue == List(req1))
      step()
      // Ensure queue is empty
      assert(elv1.pickupQueue == List())

      doSteps(8)

      // We should only have move 8 fields
      assert(elv1.getStep == 9)
      assert(elv1.getElevator(0).current == 8)
    }
  }


  test("getTargetDirection") {
    new ElevatorSystemSetUp {
      elv1.pickup(8, -1)
      step()
      assert(elv1.getElevator(0).getTargetDirection == Down)
    }
    
    new ElevatorSystemSetUp {
      elv1.pickup(8, 1)
      step()
      assert(elv1.getElevator(0).getTargetDirection == Up)
    }
    
    new ElevatorSystemSetUp {
      assert(elv1.getElevator(0).getTargetDirection == Idle)
      
      elv1.pickup(8, -1)
      doSteps(8)
      assert(elv1.getElevator(0).getTargetDirection == Idle)
    }
  }

//  test("Get nearest request") {
//    new ElevatorSystemSetUp {
//      val requests1  = List(req1, req2)
//      val requests2  = List(req2, req1)
//
//      val elv = elv1.getElevator(0)
//      assert(elv.getNearestRequest(requests1) == elv.getNearestRequest(requests2))
//    }
//  }

  test("Can pickup - up") {
    new ElevatorSystemSetUp {
      elv1.pickup(8, 1)

      step() // 1
      step() // 2
      step() // 3
      assert(elv1.getElevator(0).canPickup(PickUpRequest(5, Up, 3)))
      assert(!elv1.getElevator(0).canPickup(PickUpRequest(1, Up, 3)))
      assert(elv1.getElevator(0).canPickup(PickUpRequest(10, Up, 3)))
    }
  }
    
  test("Can pickup - down") {
    new ElevatorSystemSetUp {
      // Lets first move up a bit
      elv1.pickup(8, -1)
      doSteps(8)
      assert(elv1.getElevator(0).current == 8)
      assert(elv1.getElevator(0).goal == 8)

      // Lets go down and try to pick up along the way
      elv1.pickup(2, -1)
      doSteps(2)
      assert(elv1.getElevator(0).current == 6)
      assert(elv1.getElevator(0).goal == 2)

      assert(elv1.getElevator(0).canPickup(PickUpRequest(3, Down, 10)))
      assert(!elv1.getElevator(0).canPickup(PickUpRequest(7, Down, 10)))
      assert(elv1.getElevator(0).canPickup(PickUpRequest(1, Down, 10)))
    }
  }

  test("Test load balancing - two request, select nearest") {
    new ElevatorSystemSetUp {
      elv1.pickup(4, 1)
      elv1.pickup(7, -1)
      step()
      assert(elv1.getElevator(0).goal == 4)
    }

    new ElevatorSystemSetUp {
      elv1.pickup(7, -1)
      elv1.pickup(4, 1)
      step()
      assert(elv1.getElevator(0).goal == 4)
    }
  }
  
  test("Test load balancing - two elevators, two requests, different direction, should use both") {
    new ElevatorSystemSetUp {
      elv2.pickup(4, 1)
      elv2.pickup(5, 1)
      elv2.pickup(3, -1)
      doSteps(3)
      assert(elv2.getElevator(0).goal == 3)
      assert(elv2.getElevator(1).goal == 5)
      
      elv2.pickup(1, -1)
      step()
      assert(elv2.getElevator(0).goal == 1)
      assert(elv2.getElevator(1).goal == 5)
    }
  }
  
  test("Test load balancing - pick nearest if idling on different ends") {
    new ElevatorSystemSetUp {
      elv2.pickup(10, -1)
      doSteps(10)
      assert(elv2.getElevator(0).goal == 10 && elv2.getElevator(0).current == 10)
      elv2.pickup(4, 1)
      step()
      assert(elv2.getElevator(1).goal == 4 && elv2.getElevator(1).current == 1)
    }
  }

  test("Dropoff request") {
    new ElevatorSystemSetUp {
      elv1.pickup(5, -1)
      doSteps(5)
      assert(elv1.getElevator(0).current == 5)
      
      elv1.getElevator(0).goToFloor(1)
      doSteps(1) // 6
      
      assert(elv1.getElevator(0).queue == List(DropOffRequest(1,Down)))
      
      elv1.pickup(2, -1)
      assert(elv1.pickupQueue == List(PickUpRequest(2,Down,6)))
      
      doSteps(1) // 7
      assert(elv1.getElevator(0).queue == List(PickUpRequest(2, Down, 6), DropOffRequest(1,Down)))
      assert(elv1.getElevator(0).goal == 1)

      doSteps(1) // 8
      assert(elv1.getElevator(0).queue == List(DropOffRequest(1,Down)))

      elv1.getElevator(0).goToFloor(0)
      assert(elv1.pickupQueue == List())
      assert(elv1.getElevator(0).queue == List(DropOffRequest(0, Down), DropOffRequest(1,Down)))
      
      doSteps(1) // 9
      assert(elv1.getElevator(0).current == 1)
      assert(elv1.getElevator(0).goal == 0)
      
      doSteps(1) // 10
      assert(elv1.getElevator(0).current == 0)
    }
    
  }
}