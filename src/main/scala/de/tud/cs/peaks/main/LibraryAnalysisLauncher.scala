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
package de.tud.cs.peaks.main

import java.io.File
import org.opalj.br.analyses.Project
import de.tud.cs.peaks.capabilities.LibraryCapabilityAnalysis
import de.tud.cs.peaks.capabilities.CapabilityAnalysis
import de.tud.cs.peaks.capabilities.CapabilityAnalysis
import de.tud.cs.peaks.capabilities.CapabilitySliceAnalysis
import de.tud.cs.peaks.capabilities.Capability
import de.tud.cs.peaks.opalreports.CapabilityAnaylsisResult
import de.tud.cs.peaks.opalreports.CapabilityReport
import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.ai.domain.LogContextProvider

object LibraryAnalysisLauncher {
    
    val rtFile = new File("resources/jre_7.0_60/rt.jar")
    val rtProject = Project.apply(rtFile)
    
    def printMenu() : Unit = {
        val lineSep = System.getProperty("line.separator")
        println(
            s"$lineSep[1] Capability analysis for libraries.$lineSep" +
            s"[2] Sliced capability analysis for projects."
        )
    }
    
    def run(projectDir : File): Set[Capability] = {
        val analizer = LibraryCapabilityAnalysis
        val project = Project.apply(projectDir).extend(rtProject)
        return analizer.analyze(project) match {
            case result : CapabilityReport => return result.capabilitySet
            case _ => Set.empty[Capability]
        }
    }
    
    /**
     * 
     * -lc to lunch the Library Analysis
     * -psa to lunch the capability analysis of used library slices
     */
    def main(args : Array[String]) {
        var validInput = false
        var userInput = 0
        while(!validInput){
            printMenu()
            userInput = scala.io.StdIn.readInt()
            validInput = userInput == 1 || userInput == 2
        }
        val argsWithRTJar = args ++ Array("""-cp="resources\jre_7.0_60\rt.jar"""")
        userInput match {
            case 1 => LibraryCapabilityAnalysis.main(argsWithRTJar)
            case 2 => CapabilitySliceAnalysis.main(argsWithRTJar)
        }
    }
}