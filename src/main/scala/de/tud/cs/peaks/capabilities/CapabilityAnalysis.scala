/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.peaks.capabilities

import java.net.URL

import scala.annotation.migration
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.analyses.cg.ExtVTACallGraphAlgorithmConfiguration
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.collection.UShortSet
import de.tud.cs.peaks.opalreports.CapabilityAnaylsisResult
import de.tud.cs.peaks.extractor.CapabilityMapping
import de.tud.cs.peaks.opalreports.CapabilityReport
import de.tud.cs.peaks.opalreports.CapabilityAnaylsisResult
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.log.GlobalLogContext

import scala.collection.mutable

/** This Scala trait represents the capability inference algorithm.
  *
  * @author Michael Reif, Ben Hermann
  *
  */

trait CapabilityAnalysis extends AnalysisExecutor with OneStepAnalysis[URL, CapabilityAnaylsisResult] {

    val analysis = this

    /** List with allowed analysis parameters.
      *
      * @note Subclasses should check whether they want to support all of them, if not, the field should be overwritten.
      */
    val _ALLOWED_PARAMS = Seq("-lm", "-CL", "-CB", "-DB", "-FS", "-GU", "-IN", "-OS", "-NT", "-PR", "-RF", "-SC", "-SD", "-SY", "-UN")

    val _PARAM_MAP = Map(
        "-CL" -> Capability.ClassLoading,
        "-CB" -> Capability.Clipboard,
        "-DB" -> Capability.Debug,
        "-FS" -> Capability.Filesystem,
        "-GU" -> Capability.GUI,
        "-IN" -> Capability.Input,
        "-OS" -> Capability.Os,
        "-NT" -> Capability.Network,
        "-PR" -> Capability.Print,
        "-RF" -> Capability.Reflection,
        "-SC" -> Capability.Security,
        "-SD" -> Capability.Sound,
        "-SY" -> Capability.System,
        "-UN" -> Capability.Unsafe)

    /** @see [AnalysisExecuter#printUsage]
      */
    def printUsage = super.printUsage(GlobalLogContext)

    /** @see [AnalysisExecuter#analysisSpecificParametersDescription]
      */
    override def analysisSpecificParametersDescription: String = {
        val lineSep = System.lineSeparator()
        s"[ -lm ] - All found methods with capabilities gets listed.$lineSep" +
            s"[ -CL ] - Print all methods with the ${Capability.ClassLoading.shortForm()} capability.$lineSep" +
            s"[ -CB ] - Print all methods with the ${Capability.Clipboard.shortForm()} capability.$lineSep" +
            s"[ -DB ] - Print all methods with the ${Capability.Debug.shortForm()} capability.$lineSep" +
            s"[ -FS ] - Print all methods with the ${Capability.Filesystem.shortForm()} capability.$lineSep" +
            s"[ -GU ] - Print all methods with the ${Capability.GUI.shortForm()} capability.$lineSep" +
            s"[ -IN ] - Print all methods with the ${Capability.Input.shortForm()} capability.$lineSep" +
            s"[ -OS ] - Print all methods with the ${Capability.Os.shortForm()} capability.$lineSep" +
            s"[ -NT ] - Print all methods with the ${Capability.Network.shortForm()} capability.$lineSep" +
            s"[ -PR ] - Print all methods with the ${Capability.Print.shortForm()} capability.$lineSep" +
            s"[ -RF ] - Print all methods with the ${Capability.Reflection.shortForm()} capability.$lineSep" +
            s"[ -SC ] - Print all methods with the ${Capability.Security.shortForm()} capability.$lineSep" +
            s"[ -SD ] - Print all methods with the ${Capability.Sound.shortForm()} capability.$lineSep" +
            s"[ -SY ] - Print all methods with the ${Capability.System.shortForm()} capability.$lineSep" +
            s"[ -UN ] - Print all methods with the ${Capability.Unsafe.shortForm()} capability.$lineSep"
    }

    /** @see [AnalysisExecuter#checkAnalysisSpecificParameters]
      */
    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        parameters.filter { param => !_ALLOWED_PARAMS.contains(param) }
    }

    /** This method represents the the sub-type filter for interfaces and abstract classes which
      * would propagate dozens of capabilities all over the JRE into the given library.
      * InputStream and OutputStream are handled separately due to complicated sub type
      * relations.
      *
      * @param caller The caller method of the corresponding call.
      * @param pcs The set of program counters of the of the instructions in the method body of the caller method.
      * @param project Project of the current context.
      */
    def isInterfaceOrAbstractType(caller: Method, pcs: UShortSet, project: Project[URL]): Boolean = {
        return pcs.exists { pc =>
            caller.body.get.instructions(pc) match {
                case INVOKEINTERFACE(_, _, _) ⇒ true
                case INVOKEVIRTUAL(ObjectType("java/io/InputStream"), _, _) ⇒ true
                case INVOKEVIRTUAL(ObjectType("java/io/OutputStream"), _, _) ⇒ true
                case INVOKEVIRTUAL(rt, _, _) ⇒ checkAbstractType(caller, rt, project)
                case _ ⇒ caller.body.isEmpty || isUnimplementedMethod(caller, project)
            }
        }
    }

    /** This method checks whether the given reference type is abstract. If the caller
      * is declared in an abstract method, it have to get checked whether it is an unimplemented method.
      *
      * @param caller The caller method of the corresponding call.
      * @param rt The OPAL ReferenceType of the call receiver.
      * @param project The corresponding OPAL project.
      */
    def checkAbstractType(caller: Method, rt: ReferenceType, project: Project[URL]): Boolean = {
        if (rt.isObjectType) {
            val classFile = project.classFile(rt.asObjectType)
            if (classFile.nonEmpty && classFile.get.isAbstract)
                return isUnimplementedMethod(caller, project)
        }
        return false
    }

    /** Returns true, if the method has no body. False, otherwise.
      *
      * @param caller The caller method of the corresponding call.
      * @param project Project of the current context.
      */
    def isUnimplementedMethod(caller: Method, project: Project[URL]): Boolean = {
        var relevantClasses = project.classFile(caller).interfaceTypes
        var objectType = project.classFile(caller).superclassType
        while (objectType.nonEmpty) {
            var curCf = project.classFile(objectType.get)
            if (curCf.nonEmpty) {
                if (curCf.get.isAbstract && !relevantClasses.contains(curCf.get.thisType))
                    relevantClasses = relevantClasses.+:(curCf.get.thisType)
                relevantClasses = relevantClasses ++ curCf.get.interfaceTypes.filterNot { relevantClasses.contains(_) }
                objectType = curCf.get.superclassType
            } else objectType = Option.empty[ObjectType]
        }
        for (it ← relevantClasses) {
            val classFile = project.classFile(it)
            if (classFile.nonEmpty && classFile.get.findMethod(caller.name).nonEmpty)
                return true
        }
        return false;
    }

    /** Return true if the given method is called on java/lang/Object.
      *
      * @param caller The calling Method of the call.
      * @param pcs The set of program counters of the of the instructions in the method body of the caller method.
      */
    def nonObjectCall(caller: Method, pcs: UShortSet): Boolean = {
        return pcs.exists { pc =>
            caller.body.get.instructions(pc) match {
                case INVOKEVIRTUAL(ObjectType.Object, _, _) ⇒ false
                case _ ⇒ true
            }
        }
    }

    /** Filters methods which are either calls on java/lang/Object, an interface type or an abstract class.
      * The filter only applies for callees within the rt.jar
      *
      * @param callee The target method of the call.
      * @param caller The caller method of the call.
      * @param pcs The set of program counters of the of the instructions in the method body of the caller method.
      * @param project The corresponding OPAL project.
      */
    def filterMethod(callee: Method, caller: Method, pcs: UShortSet, project: Project[URL]): Boolean = {
        val calleeCF = project.classFile(callee)
        val callerCF = project.classFile(caller)
        val isCallOnObject = nonObjectCall(caller, pcs)
        var result = true
        if (isJclSourceByClass(calleeCF, project)) {
            if (isInterfaceOrAbstractType(caller, pcs, project))
                result = calleeCF.fqn.splitAt(calleeCF.fqn.lastIndexOf("/"))._1.equals(callerCF.fqn.splitAt(callerCF.fqn.lastIndexOf("/"))._1)
            result && isCallOnObject
        } else
            result
    }

    /** Return true, if the given method is declared within the 'rt.jar'.
      *
      * @param method The method under test if it is a method of the JCL.
      * @param project The corresponding OPAL project.
      */
    def isJclSource(method: Method, project: Project[URL]): Boolean = {
        isJclSourceByClass(project.classFile(method), project)
    }

    /** Return true, if the given class file is declared within the 'rt.jar'.
      *
      * @param classFile The class file which should be checked whether it's defined inside the JCL.
      * @param project The corresponding OPAL project.
      */
    def isJclSourceByClass(classFile: ClassFile, project: Project[URL]): Boolean = {
        project.source(ObjectType(classFile.fqn)).get.toString().contains("resources/jre_7.0_60/rt.jar")
    }

    /** Returns the CallGraph of the given project.
      *
      * @param project This project is the base of the call graph construction.
      */
    def buildCallGraph(project: Project[URL]): CallGraph = {
        CallGraphFactory.create(project,
            () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(project),
            new ExtVTACallGraphAlgorithmConfiguration(project)).callGraph
    }

    /** Returns all native methods that can be found in the project.
      *
      * @param The OPAL project of the library under analysis.
      */
    def getNativeMethods(project: Project[URL]): Iterable[Method] = {
        return for {
            cf ← project.allClassFiles if isJclSourceByClass(cf, project)
            m ← cf.methods if m.isNative
        } yield m
    }

    /** This methods seeds the given map with the identity capabilities.
      *
      * @param nativeMethods Iterable of OPAL methods which contains the list of native methods.
      * @param capMap The map that have to be seeded.
      * @param project The corresponding OPAL project.
      */
    def seedIdentityCaps(nativeMethods: Iterable[Method], capMap: HashMap[Method, HashSet[Capability]], project: Project[URL]): Unit = {
        nativeMethods.foreach{ n =>
            capMap.put(n, CapabilityMapping.getCapability(n, project))
        }
    }

    /** Returns a set with all transitive methods included. The capabilities get propageted as side effect.
      *
      * @param nativeMethods Iterable of the native methods.
      * @param capMap The seeded capability map.
      * @param callGraph The call graph of the project.
      * @param project Project of the current context.
      */
    def calulateTransitiveHull(nativeMethods: Iterable[Method],
        capMap: HashMap[Method, HashSet[Capability]],
        callGraph: CallGraph,
        project: Project[URL]): scala.collection.mutable.Set[Method] = {
        val result = new HashSet[Method]()
        result ++= nativeMethods.toList

        val workQueue = new Queue[Method]()
        workQueue ++= nativeMethods.toList.sortBy( m => m.toJava(project.classFile(m)))

        while (workQueue.nonEmpty) {
            val currentMethod = workQueue.dequeue

            val calledBy = callGraph.calledBy(currentMethod)
            if (calledBy.nonEmpty) {
                val callers = calledBy.filter { case (method, pcs) ⇒ filterMethod(currentMethod, method, pcs, project) }

                for { caller ← callers.keySet } {
                    var capSet = capMap.getOrElse(caller, HashSet.empty[Capability])
                    capSet = capSet ++ capMap.getOrElse(currentMethod, HashSet.empty[Capability])
                    capMap.put(caller, capSet)
                }

                val newCallers = callers.keySet.filterNot { result.contains(_) }
                result ++= newCallers
                workQueue ++= newCallers.toList.sortBy( m => m.toJava(project.classFile(m)))
            }
        }

        result
    }

    /** Describes the filter which is used to create the correct report. This analysis filters every JCL method.
      */
    def filterResults = isJclSource _

    /** Returns every  - method, capability set - tuple that is relevant for the report.
      *
      * @param transitiveHull All methods that can transitively reach native calls.
      * @param capMap      The map that contains the capabilities.
      * @param listMethods True, and every library method with capabilities is printed with the according capability set.
      *                   False, nothing happens.
      * @param project The corresponding OPAL project.
      */
    def getReportTuples(transitiveHull: scala.collection.mutable.Set[Method],
        capMap: HashMap[Method, HashSet[Capability]],
        project: Project[URL]): mutable.Set[(Method, mutable.HashSet[Capability])] = {
        for {
            method ← transitiveHull.filterNot { filterResults(_, project) }
            capSet ← capMap.get(method) if isValidCapset(capSet)
        } yield (method, capSet)
    }

    /** Returns True, if the given capSet is not empty and does not only contain the 'native' capability.
      *
      * @param capSet A set of capabilities.
      */
    protected def isValidCapset(capSet: HashSet[Capability]): Boolean = {
        capSet.nonEmpty && !(capSet.size == 1 && capSet.contains(Capability.Native))
    }

    /** Prints all methods or if the param 'filterSet' is nonEmpty, it prints only methods that belong to
      * one Capability in the specified set.
      *
      * @param methodsWithCapabilities Set of tuples of a OPAL Method
      *       and a Set of Capabilities which were identified by the capability inference algorithm.
      * @param filterSet A Set of Capabilities which is used to filter interesting methods.
      */
    protected def printMethods(methodsWithCapabilities: Set[(Method, HashSet[Capability])], filterSet: Set[Capability], project: Project[URL]): Unit = {
        def formatMethod(method: Method, capSet: HashSet[Capability], project: Project[URL]): String = project.classFile(method).fqn + "  -  " + method.toString() + " => " + capSet.map(x ⇒ x.shortForm()).mkString("[", ", ", "]")

        for ((method, capSet) <- methodsWithCapabilities)
            if (filterSet.nonEmpty) {
                if (capSet.intersect(filterSet).size > 0)
                    println(formatMethod(method, capSet, project))
            } else
                println(formatMethod(method, capSet, project))
    }

    /** This method triggers the capability inference analysis.
      *
      * @see [OneStepAnalysis#doAnalize]
      */
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean): CapabilityAnaylsisResult = {

        val methodsWithCapabilities: Iterable[(Method, HashSet[Capability])] = computeCapabilities(project)

        val capFilter = _PARAM_MAP.collect { case (key, value) if parameters.contains(key) => value }
        val listMethods = capFilter.nonEmpty || parameters.contains("-lm")
        if (listMethods) printMethods(methodsWithCapabilities.toSet, capFilter.toSet, project)

        CapabilityReport(methodsWithCapabilities.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2)))
    }

    def computeCapabilities(project: Project[URL]): Iterable[(Method, HashSet[Capability])] = {
        val capMap = HashMap.empty[Method, HashSet[Capability]]

        val nativeMethods = getNativeMethods(project)

        seedIdentityCaps(nativeMethods, capMap, project)

        val callGraph = buildCallGraph(project)
        val transitiveHull = calulateTransitiveHull(nativeMethods, capMap, callGraph, project)
        val methodsWithCapabilities = getReportTuples(transitiveHull, capMap, project)
        val overAllCaps = capMap.foldLeft(0)((acc, pair) => acc + pair._2.size)

        methodsWithCapabilities
    }
}