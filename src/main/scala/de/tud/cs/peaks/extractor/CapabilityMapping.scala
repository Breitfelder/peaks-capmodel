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

import scala.collection.mutable.HashMap
import org.opalj.br.Method
import scala.io.Source
import scala.collection.mutable.HashSet
import java.io.IOException
import java.io.FileNotFoundException
import org.opalj.br.analyses.Project
import java.net.URL
import de.tud.cs.peaks.capabilities.Capability

/**
 *
 * This objects provides a mapping form the capabilities in the corresponding csv to the internal capabilities.
 *
 * @author Michael Reif
 */
object CapabilityMapping {

    //header = package, class, method name, return type, parameters, capabilities
    val _nativeCallCSV = "resources/capabilities/NativeMethodsRT.csv"
    var capMap = HashMap.empty[TMethod, HashSet[Capability]]
    val stringToCapMap = getStringToCapMap()

    /**
     * Returns a set of capabilities used by the given method. This is necessary to
     * synchronize the OPAL methods of the current analysis with the up front calculated
     * native methods.
     *
     * @param method Should be native method of the rt.jar.
     * @param theProject the Project of the used analysis
     */
    def getCapability(method: Method, theProject: Project[URL]): HashSet[Capability] = {
        if (capMap.isEmpty)
            init()

        return capMap.getOrElse(MethodTransformation.getTMethod(theProject, method), HashSet.empty[Capability])
    }

    /**
     * Initializes the capability map from the 'NativeMethodsRT.csv' file.
     */
    def init(): Unit = {
        try {
            val lines = Source.fromFile(_nativeCallCSV).getLines()
            for (line ← lines.drop(1)) {
                val (method, capSet) = parseCsvRow(line)
                capMap.put(method, capSet)
            }
        } catch {
            case ex: FileNotFoundException ⇒ println(s"Couldn't find that file: ${_nativeCallCSV}")
            case ex: IOException           ⇒ println(s"Had an IOException trying to read that file: ${_nativeCallCSV}")
        }
    }

    /**
     * Returns a map with String to capability key values pairs. It is a simple lookup table.
     */
    def getStringToCapMap(): HashMap[String, Capability] = {
        HashMap(
            "FS" -> Capability.Filesystem,
            "NET" -> Capability.Network,
            "NATIVE" -> Capability.Native,
            "SOUND" -> Capability.Sound,
            "UNSAFE" -> Capability.Unsafe,
            "PRINT" -> Capability.Print,
            "SYSTEM" -> Capability.System,
            "CLASSLOADING" -> Capability.ClassLoading,
            "REFLECTION" -> Capability.Reflection,
            "SECURITY" -> Capability.Security,
            "CLIPBOARD" -> Capability.Clipboard,
            "OS" -> Capability.System,
            "INPUT" -> Capability.Input,
            "DEBUG" -> Capability.Debug,
            "GUI" -> Capability.GUI)
    }

    /**
     * Returns a tuple of the encapsulated method and its capability set.
     *
     * @param line Line from the 'NativeMethodsRT.csv'.
     */
    def parseCsvRow(line: String): (TMethod, HashSet[Capability]) = {
        val entries = line.replaceAll(" ", "").split(";")
        val packageName = entries(0).replaceAll("[.]", "/")
        val className = entries(1)
        val methodName = entries(2)
        val returnType = entries(3)
        val parameters = entries(4)
        val capabilities = entries(5)

        var finalParameterList = parseParameterList(parameters)
        var finalCapabilitySet = parseCapabilitySet(capabilities)

        return (TMethod(packageName, className, methodName,
            returnType, finalParameterList), finalCapabilitySet)
    }

    /**
     * Returns the capability set which is contained in the given String.
     *
     * @param capabilities List of comma separated capabilities.
     */
    def parseCapabilitySet(capabilities: String): HashSet[Capability] = {
        val result = HashSet.empty[Capability]
        if (capabilities.length() == 0)
            return result
        else if (capabilities.contains(",")) {
            return capabilities.split(",").foldLeft(result)((result, x) ⇒ HashSet.empty[Capability].+(stringToCapMap.getOrElse(x, Capability("Unknown"))))
        } else
            return result ++ stringToCapMap.get(capabilities)
    }
    
    /**
     * Returns the parameter list of a method.
     * 
     * @param parameters List of parameters in a comma separated String.
     */
    def parseParameterList(parameters: String): List[String] = {
        if (parameters.contains(","))
            return parameters.split(",").toList
        else
            return List.empty[String].+:(parameters)
    }
}