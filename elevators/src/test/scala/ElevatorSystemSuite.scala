/**
 * Created by urbanskudnik on 14/01/15.
 */
import org.junit.runner.RunWith
import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ElevatorSystemSuite extends FunSuite {
  trait ElevatorSystemSetUp {
    val elv1 = new ElevatorControlSystem(1)
    val elv2 = new ElevatorControlSystem(2)
    
    def step() = {
      elv1.step()
      elv2.step()
    }
    
    def status() = {
      println("\nStep:    " + elv1.getStep)
      println("---- ---- ---- ----")
      println("Queue 1:    " + elv1.pickupQueue.toList)
      println("Queue 2:    " + elv2.pickupQueue.toList)
      println("---- ---- ---- ----")
      println("Elevator system 1:    " + elv1.status.toList)
      println("Elevator system 2:    " + elv2.status.toList)
      println("---- ---- ---- ----")
    }
  }
  
  test("Working status") {
    new ElevatorSystemSetUp {
      assert(elv2.status.toList == List((1,0,0), (2,0,0)), "Status does not appear to operate nominally.")
    }
  }
  
  test("Test pickup requests") {
    new ElevatorSystemSetUp {
      elv1.pickup(2, -1)
      elv2.pickup(1, 1)
      elv2.pickup(3, -1)

      // We populated the queue, but everything should be as it was at start
        assert(elv1.status.toList == List((1, 0, 0)))
        assert(elv2.status.toList == List((1,0,0), (2,0,0)))

        assert(elv1.pickupQueue.toList == List((2,-1, 0)))
        assert(elv2.pickupQueue.toList == List((1, 1, 0), (3, -1, 0)))
      
      // First step, elevators should move one step to their final destination
      step()
      assert(elv1.status.toList == List((1, 1, 2)))
      assert(elv2.status.toList == List((1,1,1), (2,1,3)))
      
      // Queue should be empty
      assert(elv1.pickupQueue.toList == List())
      assert(elv2.pickupQueue.toList == List())

      step()
      assert(elv1.status.toList == List((1, 2, 2)))
      assert(elv2.status.toList == List((1,1,1), (2,2,3)))

      step()
      assert(elv1.status.toList == List((1, 2, 2)))
      assert(elv2.status.toList == List((1,1, 1), (2,3,3)))
    }
  }


  test("Low number elevators, high number reqs") {
    new ElevatorSystemSetUp {
      elv2.pickup(1, 1)
      elv2.pickup(3, -1)
      elv2.pickup(5, 1)
      
      assert(elv2.pickupQueue.toList == List((1, 1, 0), (3, -1, 0), (5, 1, 0)))
      
      step() // 1
      
      assert(elv2.pickupQueue.toList == List((5, 1, 0)))
      elv2.pickup(1, 1)
      assert(elv2.pickupQueue.toList == List((5, 1, 0), (1, 1, 1)))
      status()
      step() // 2
      assert(elv2.pickupQueue.toList == List((1, 1, 1)))
      step() // 3
      step() // 4
      assert(elv2.pickupQueue.toList == List())
      step() // 5
      assert(elv2.status.toList == List((1,5, 5), (2,1,1)))
    }
  }
  
  test("Load balancing considering distance") {
    new ElevatorSystemSetUp {
      elv2.pickup(1, 1)
      elv2.pickup(10, 1)
      (1 to 10).map(x => step())
      assert(elv2.status.toList == List((1,1, 1), (2,10,10)))
      elv2.pickup(3, -1)
      step()
      assert(elv2.status.toList == List((1,2, 3), (2,10,10)))
      step()
      assert(elv2.status.toList == List((1,3, 3), (2,10,10)))
      elv2.pickup(7, -1)
      step()
      assert(elv2.status.toList == List((1,3, 3), (2,9,7)))
    }
  }
}