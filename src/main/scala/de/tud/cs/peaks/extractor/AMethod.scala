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

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import java.net.URL

/**
 * Trait for an intermediate data structure to parse methods from and to files.
 *
 * @author Michael Reif
 *
 */
trait AMethod {
    def packageName: String
    def className: String
    def methodName: String
    def returnType: String
    def parameterList: List[String]

    def paramsAsString(): String = {
        parameterList.mkString(",")
    }
    def toStringAsValue(): String = {
        "('"+Seq(methodName, packageName, className, returnType, parameterList.mkString(",")).mkString("','")+"')"
    }
}

/**
 * Case class which encapsulates a method which can be uniquely identified.
 */
case class TMethod(
    override val packageName: String,
    override val className: String,
    override val methodName: String,
    override val returnType: String,
    override val parameterList: List[String]) extends AMethod

/**
 * Provides methods to transform simple Strings to an Method object.
 */
object MethodTransformation {

    /**
     *  Returns a Method described by Strings.
     *
     * @param p Package where the method is defined.
     * @param c Class name where the method is defined.
     * @param m Name of the method.
     * @param r Return type of the method.
     * @param pa Parameter list of the method in a comma separated String.
     */
    def getTMethod(p: String, c: String, m: String, r: String, pa: String): TMethod = {
        var params = List.empty[String]
        if (pa.length() > 0) {
            params = params.++(pa.split(","))
        }
        return TMethod(p, c, m, r, params)
    }
    
    /**
     * Returns a method described by Strings, build from the internal OPAL representation.
     * 
     * @param theProject The current OPAL project.
     * @param method The OPAL representation of the method.
     */
    def getTMethod(theProject: Project[URL], method: Method): TMethod = {

        val path = method.classFile.fqn
        val packageClassDivider = path.lastIndexOf("/")

        return TMethod(path.substring(0, packageClassDivider), path.substring(packageClassDivider + 1), method.name, method.returnType.toJava,
            method.parameterTypes.toList.map(param ⇒ param.toJava))
    }
}