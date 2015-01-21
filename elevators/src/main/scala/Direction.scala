object Direction extends Enumeration {
  type Direction = Value
  val Up, Down, Idle = Value

  def intToDirection(num: Int): Direction = {
    if (num > 0) Up
    else if (num < 0) Down
    else Idle
  }
}