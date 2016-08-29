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

import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.ai.analyses.cg.ExtVTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.br.analyses.Project
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import org.opalj.br.Method
import java.net.URL
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.ai.analyses.cg.ComputedCallGraph
import scala.collection.mutable.TreeSet
import org.opalj.ai.analyses.cg.DefaultVTACallGraphAlgorithmConfiguration
import org.opalj.UShort
import org.opalj.collection.UShortSet
import org.opalj.br.instructions.Instruction
import org.opalj.br.ObjectType
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.ClassFile
import org.opalj.br.MethodDescriptor
import org.opalj.br.SingleArgumentMethodDescriptor
import org.opalj.br.ReferenceType
import de.tud.cs.peaks.extractor.CapabilityMapping
import de.tud.cs.peaks.opalreports.CapabilityReport

/**
 * This is an very alpha version to understand the call chain of a from a given method to the
 * different natives methods of a given capability, e.g. 'CLASSLOADING'.
 * 
 * @note There is no interface to trigger it from the outside. Commandline parameters
 *       would look like -cp="libpath" -cp="resources/jre_7.0_60/rt.jar".
 *       !!! Either the target method and the capability under test have to be specified in the source code !!! 
 *      
 * @author Michael Reif   
 */
object CallTreeOfMethod extends CapabilityAnalysis{
    
        override def title: String = "Call tree construction of a method with a capability."

        override def description: String =
            "First identify all native calls than propagate capabilities to the library and build the call tree of a method afterwards."
            

        override def filterMethod(callee: Method, caller: Method, pcs: UShortSet, project: Project[URL]): Boolean = true
        
        override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean) = {
            
            val capMap = HashMap.empty[Method, HashSet[Capability]]
            
            /*
             * Alpha data structure to explore call chains.
             *  Calls are printed as trees, but due to the fact that we have an cyclic call graph
             *  are not all calls included. 
             */
            val alreadyVisited = HashSet.empty[Method]
            class CallTree(val value: Method, val cap: Capability) {

                var children = List.empty[CallTree]

                def addChild(child: CallTree): Unit = {
                    children = children.+:(child)
                }

                def calculateChildren(cg: CallGraph): Unit = {
                    val callees = cg.calls(value)
                    alreadyVisited.+=(value)
                    var tmp = List.empty[Method]
                    for (pc ← callees.keySet) {
                        for (method ← callees.get(pc).get) {
//                            if (capMap.get(method).getOrElse(Set.empty[Capability]).contains(cap))
                                tmp = tmp.+:(method)
                        }
                    }
                    for (method ← tmp) {
                        if (!alreadyVisited.contains(method)) {
                            val tree = new CallTree(method, cap)
                            tree.calculateChildren(cg)
                            addChild(tree)
                        }
                    }
                }

                def print(): Unit = {
                    alreadyVisited.clear()
                    pprint("", true)
                }

                def pprint(prefix: String, isLeaf: Boolean): Unit = {
                    if (!alreadyVisited.contains(value)) {
                        val text = s"$prefix ${getParentSymbol(isLeaf)} ${project.classFile(value).fqn+" "+value.toString()}"
                        println(text)
                        alreadyVisited.+=(value)
                        for (child ← children) {
                            child.pprint(s"$prefix ${getChildrenSymbol(isLeaf)}", child eq children.last)
                        }
                    } else println(s"$prefix ${getParentSymbol(isLeaf)} linked to --> ${project.classFile(value).fqn+" "+value.toJava}")
                }

                def getParentSymbol(isLeaf: Boolean): String = {
                    if (isLeaf) return "└── "
                    else return "├── "
                }

                def getChildrenSymbol(isLeaf: Boolean): String = {
                    if (isLeaf) return "    "
                    else return "│   "
                }

                def isLeaf = children.isEmpty
            }
            
            // ### internal data structure ends. ###
            
            val nativeMethods = getNativeMethods(project)

            seedIdentityCaps(nativeMethods, capMap, project)

            val callGraph = buildCallGraph(project)

            val transitiveHull  = calulateTransitiveHull(nativeMethods, capMap, callGraph, project)
            
            /* full qualified name of a class in JVM notation */
            val fqn = "java/util/zip/DeflaterOutputStream"
            /* name of the method which should be shows. (should be included in the upper class) */
            val name = "flush"
            /* capability you are interested in */
            val cap = Capability.Filesystem

            val theOneMethod = {
                for {
                    cf ← project.allClassFiles if cf.fqn == fqn
                    m ← cf.methods if m.name == name
                } yield m
            }
            
            if(theOneMethod.size > 0){
                val tree = new CallTree(theOneMethod.head, cap)
                tree.calculateChildren(callGraph)
                tree.print()
            } else println(Console.RED + s"$fqn $name not found! Please check for typos." + Console.RESET)
            
            CapabilityReport(Set(Capability("All shown methods have the capability: "),cap))
            //CapabilityReport(capMap.values.fold(Set.empty[Capability])((r,v) => r ++ v))
        }
}