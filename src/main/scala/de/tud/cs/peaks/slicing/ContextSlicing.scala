package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.LibraryCapabilityAnalysis
import org.opalj.ai.analyses.cg.{CHACallGraphAlgorithmConfiguration, CallGraph, CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}
import org.opalj.br.{ClassFile, Method, ObjectType}
import org.opalj.br.analyses.Project

import scala.collection.mutable

trait ContextSlicing {
  def sliceByContext(project: Project[URL], appContext: Set[String]): Map[ClassFile, Set[Method]] = {
    // 1. determine application

    def toJarSource(u: URL): String = u.toString().substring("jar:file:".length, u.toString().indexOf("!"))


    val projectSources: Iterable[String] = project.projectClassFilesWithSources.map(e => toJarSource(e._2)).toSet.filterNot(s => s.contains("resources/jre_7.0_60/rt.jar"))

    val libSources: Set[String] = projectSources.filterNot(s => appContext.contains(s)).toSet
    val appSources: Set[String] = projectSources.filter(s => appContext.contains(s)).toSet

    val cg = buildCallGraph(project)

    // determine all call into lib from app sources
    var appClasses = project.projectClassFilesWithSources.collect({ case (c, s) if appSources.contains(toJarSource(s)) => c })

    var appMethods: Iterable[Method] = appClasses.map(c => c.methods).flatten
    var appMethodCalls: Iterable[Method] = appMethods.map(m => cg.calls(m).map(_._2).flatten).flatten
    val appToLibCalls: Iterable[Method] = appMethodCalls.filter(m => libSources.contains(toJarSource(project.source(ObjectType(project.classFile(m).fqn)).get)))

    println("appMethod: " + appMethods.size + " - appMethodCalls: " + appMethodCalls.size + " - appToLibCalls: " + appToLibCalls.size)

    appToLibCalls.foreach(m => println(m.toJava(project.classFile(m))))

    val result = new mutable.HashSet[Method]()
    result ++= appToLibCalls

    result ++= computeNecessaryMethods(appToLibCalls.map(project.classFile(_)).toSet)

    def computeNecessaryMethods(classes : Set[ClassFile]) : Set[Method] = {
      classes.map(c => c.methods).flatten.filter(m => m.isStaticInitializer || m.isConstructor)
    }


    val workQueue = new mutable.Queue[Method]()
    workQueue ++= result.toList.sortBy(m => m.toJava(project.classFile(m)))

    while (workQueue.nonEmpty) {
      val current = workQueue.dequeue()

      val newCalls = cg.calls(current).map(_._2).flatten.filter(m => libSources.contains(toJarSource(project.source(ObjectType(project.classFile(m).fqn)).get)) && !result.contains(m))

      result ++= newCalls
      //result ++= computeNecessaryMethods(newCalls.map(project.classFile(_)).toSet)
      workQueue ++= newCalls
    }


    println("Final result: " + result.size)

    result.toSet.groupBy(m => project.classFile(m))
  }

  /** Returns the CallGraph of the given project.
    *
    * @param project This project is the base of the call graph construction.
    */
  private def buildCallGraph(project: Project[URL]): CallGraph = {
    CallGraphFactory.create(project,
      () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(project),
      new CHACallGraphAlgorithmConfiguration(project)).callGraph
  }
}
