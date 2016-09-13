package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.Capability
import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.Project

object CapabilityAndContextSlicer extends Slicer with CapabilitySlicing with ContextSlicing  {
  override def title: String = "Slices a library down to methods within the definded capability scope."

  override def description: String =
    "Produces a library slice based on a set of desired/allowed capabilities."

  def computeSlice(project: Project[URL], caps : Set[Capability], appContext : Set[String]) : Map[ClassFile, Set[Method]] = {
    val capSlice = sliceByCapSet(project, caps)
    val contextSlice = sliceByContext(project, appContext)

    capSlice.map(e => e._2).flatten.toSet.intersect(contextSlice.map(e => e._2).flatten.toSet).groupBy(m => project.classFile(m))
  }

}
