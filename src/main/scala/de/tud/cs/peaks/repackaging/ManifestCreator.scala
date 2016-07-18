package de.tud.cs.peaks.repackaging

object ManifestCreator {
  def createFromAncestor(m : java.util.jar.Manifest) : java.util.jar.Manifest = {
    val result = new java.util.jar.Manifest(m)
    val att = result.getAttributes("Peaks")
    att.putValue("Minimized", "true")
    return result
  }
}