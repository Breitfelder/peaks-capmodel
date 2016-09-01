package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.{Capability, LibraryCapabilityAnalysis}
import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.Project


trait CapabilitySlicing {

  def sliceByCapSet(project: Project[URL], caps: Set[Capability]): Map[ClassFile, Set[Method]] = {
    println("Desired capability footprint: " + caps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    // 1.1 Determine current footprint

    val methodsWithCapabilities = LibraryCapabilityAnalysis.computeCapabilities(project)
    val usedCaps = methodsWithCapabilities.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Currently used capabilities: (" + methodsWithCapabilities.size + " method(s)) " + usedCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    // 1.2 Filter undesired methods

    val filtered = methodsWithCapabilities.filter(mcaps => (mcaps._2 -- caps).isEmpty)
    val filteredCaps = filtered.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Capabilities after slicing: (" + filtered.size + " method(s)) " + filteredCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    val removed = methodsWithCapabilities.filter(mcaps => (mcaps._2 -- caps).nonEmpty)
    val removedCaps = removed.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Capabilities removed: (" + removed.size + " method(s)) " + removedCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    removed.foreach(m => println(m._1.toJava(project.classFile(m._1))))

    val libraryMethods = project.methods().filter(m => !LibraryCapabilityAnalysis.isJclSource(m, project))

    println("Complete library method count: " + libraryMethods.size)

    val newSlice = libraryMethods.toSet -- removed.map(tuple => tuple._1).toSet
    println("New slice method count: " + newSlice.size)

    val slice: Map[ClassFile, Set[Method]] = newSlice.groupBy(m => project.classFile(m))
    slice
  }

}
