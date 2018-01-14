package de.tud.cs.peaks.capabilities

object CapMapper {
  val validCapabilities = Array("FS", "GUI", "NET", "SYSTEM", "CLIPBOARD")

  /**
   * Returns true if the given capability name is valid.
   */
  def isValidCap(cap: String): Boolean = {
    return validCapabilities.contains(cap)
  }

  /**
   * Maps a peaks capability to the external output format
   */
  def mapCap(cap: String): String = {
    cap match {
      case "FS" => return "filesystem"
      case "GUI" => return "monitor"
      case "SYSTEM" => return "sensor"
      case "CLIPBOARD" => return "clipboard"
      case "NET" => return "network"
      case _ => return "unknown cap"
    }
  }
}