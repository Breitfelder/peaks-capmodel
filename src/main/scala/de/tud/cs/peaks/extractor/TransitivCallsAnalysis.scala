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
package de.tud.cs.peaks.extractor

import java.net.URL

import scala.collection.mutable.HashMap

import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.ai.analyses.cg.DefaultVTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.ExtVTACallGraphAlgorithmConfiguration
import org.opalj.br.Method
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.collection.UShortSet

import de.tud.cs.peaks.capabilities.Capability
import de.tud.cs.peaks.capabilities.CapabilityAnalysis
import de.tud.cs.peaks.opalreports.StatisticalReport
import de.tud.cs.peaks.opalreports.StatisticalReport

/**
 * This object does analyze the some library or project to figure out
 * all transitive native calls. All static initializers, every non-private
 * static method, constructor and method are used as entry points for the
 * call graph analysis.
 */
object TransitivCallsAnalysis extends CapabilityAnalysis {

    override def title: String = "Get all transitive native calls of the library/project."

    override def description: String =
            "First identify all native calls then harvest all transitive native calls."
    
    
    /**
     * Returns false, this analysis does not apply any filter.
     * 
     * @note see[CapabilityAnalysis#filterResults]
     */
    override def filterResults = (_,_) => false
    
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean) = {

        val capMap = HashMap.empty[Method, Set[Capability]]

        val nativeMethods = getNativeMethods(project)

        seedIdentityCaps(nativeMethods, capMap, project)

        val callGraph = buildCallGraph(project)

        val transitiveHull = calulateTransitiveHull(nativeMethods, capMap, callGraph, project)
        
        val methodsWithCapabilities = getReportTuples(transitiveHull, capMap, project)
        
        val capFilter = _PARAM_MAP.collect{ case (key, value) if parameters.contains(key) => value }
        val listMethods = capFilter.nonEmpty || parameters.contains("-lm")
        if(listMethods) printMethods(methodsWithCapabilities.toSet, capFilter.toSet, project)
        
        StatisticalReport("")
    }
}