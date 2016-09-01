package de.tud.cs.peaks.slicing
import java.net.URL

import de.tud.cs.peaks.capabilities.Capability
import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.Project

object CapabilitySlicer extends Slicer with CapabilitySlicing {
  override def title: String = "Slices a library down to methods within the definded capability scope."

  override def description: String =
    "Produces a library slice based on a set of desired/allowed capabilities."

  def computeSlice(project: Project[URL], caps : Set[Capability]) : Map[ClassFile, Set[Method]] = sliceByCapSet(project, caps)
}
