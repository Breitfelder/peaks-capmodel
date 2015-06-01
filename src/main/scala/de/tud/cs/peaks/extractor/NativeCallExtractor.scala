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

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.OneStepAnalysis

/**
 * This object provides a analysis which can extract the native calls of a given library.
 */
object NativeCallExtractor extends AnalysisExecutor {

    val file = "output\\extractedNativeMethods.csv"

    val header = Array[String]("package", "class", "method", "return", "parameters", System.lineSeparator())

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String = "Extractes the native methods of the given library and writes them"+
            s"into $file ."

        def doAnalyze(project: Project[URL], parameters: Seq[String] = List.empty, isInterrupted: () ⇒ Boolean) = {
            val nativeMethods =
                for {
                    classFile ← project.allClassFiles;
                    method ← classFile.methods if method.isNative
                } yield classFile.thisType.toJava+"{ "+method.toJava+" }"

            val writer = new BufferedWriter(new FileWriter(new File(file)))
            writer.write(header.mkString(";"))

            //header: package, class, method, return, parameters
            //ex: com.sun.deploy.util.WinRegistry{ com.sun.deploy.util.WinRegistry$KeyValue sysQueryKey(int,java.lang.String) }
            for (method ← nativeMethods) {
                val path = method.split("[{]")
                val row = new Array[String](6)

                row(0) = path(0).substring(0, path(0).lastIndexOf("."))
                row(1) = path(0).substring(path(0).lastIndexOf(".") + 1)
                row(2) = path(1).trim().split("[(]")(0).split("[ ]")(1)
                row(3) = path(1).trim().split("[ ]")(0)
                row(4) = path(1).split("[(]")(1).replaceAll("[)}]", "")
                row(5) = System.lineSeparator()

                writer.write(row.mkString(";"))
            }

            writer.flush()
            writer.close()

            BasicReport(
                nativeMethods.size+" native methods found:"+
                    nativeMethods.mkString("\n\t", "\n\t", "\n")
            )
        }
    }
}