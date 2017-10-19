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

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project


/**
 * This analysis takes the JCL and the used part of the library into account while
 * creating the capability footprint.
 * 
 * @note This analysis does only make sense if the there is an addition application specified. Use
 * the "-cp" option to specify an additional application.
 * 
 * @author Michael Reif
 */
object CapabilitySliceAnalysis extends CapabilityAnalysis {

    override def title: String = "Determine the capability set which is used by the given project. Only used slices were considered"

    override def description: String =
        "Finds the capability footprint of a given project considering additional used libraries or the 'rt.jar'"

    /**
     * Return True, when the given method is not declared in the Project but in the rt.jar or a third party library instead.
     *        False, otherwise.
     * @param method The method, which Source should be checked.
     * @param project The corresponding OPAL project.
     * 
     * @note current assumption: dependencies are in the same jar as the project. (as sub jars)
     */
    def nonProjectSource(method: Method, project: Project[URL]): Boolean = {
        val classFile = method.classFile
        val src = project.source(ObjectType(classFile.fqn)).get.toString().split("!")
        return src.size > 2 || isJclSource(method, project)
    }
    
    /**
     * @see [CapabilityAnalysis#filterResults]
     */
    override def filterResults = nonProjectSource _
}