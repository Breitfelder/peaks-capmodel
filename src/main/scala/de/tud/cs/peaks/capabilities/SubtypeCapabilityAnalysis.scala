package de.tud.cs.peaks.capabilities

import java.net.URL

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL

/**
 * Performs a subtype analysis on the
 * results of the capability analysis
 */
class SubtypeCapabilityAnalysis(project: Project[URL], capMap: HashMap[Method, HashSet[Capability]], capabilityAnalysisResults: Iterable[AnalysisResult]) {

  var CapabilitiesSubtypeAnalysis = ListBuffer.empty[AnalysisResult]

  /**
   * hashtable attributes
   * [KEY         , VALUE                     ]
   * [called class, (called methods, subtypes)]
   */
  var subtypes = new java.util.Hashtable[(String, String), (List[Method], List[String])]

  /**
   * Method to start the capability analysis for known subtypes.
   */
  def startAnalysis(): Iterable[AnalysisResult] = {
    // loop over the found capability analysis results
    var i = 0
    var max = capabilityAnalysisResults.size

    for (analysisResult <- capabilityAnalysisResults) {

      val cls = analysisResult.analyzedMethod.classFile
      val fields = cls.fields
      val constructors = cls.methods.filter(method => method.isConstructor && method.parameterTypes.nonEmpty)

      var map = scala.collection.mutable.Map.empty[String, Set[(Method, Int)]]

      for (field <- fields) {
        var set = Set.empty[(Method, Int)]

        for (c <- constructors.filter(c => c.parameterTypes.contains(field.fieldType))) {
          set += ((c, c.parameterTypes.indexOf(field.fieldType)))
        }

        if (set.nonEmpty) map += field.name -> set
      }

      var listOfSubtypes = ListBuffer.empty[String]
      i = i + 1
      printf(i + "/" + max + "\n")

      // loop over method instructions
      for ((pc, instruction) <- analysisResult.analyzedMethod.body.get.associateWithIndex()) {
        var calledClass: ObjectType = null
        var calledMethod: String = null
        var calledMethodDescriptor: MethodDescriptor = null

        instruction match {
          case INVOKEINTERFACE(declaringClass, methodName, methodDescriptor) => {
            calledClass = declaringClass
            calledMethod = methodName
            calledMethodDescriptor = methodDescriptor
          }
          case INVOKESPECIAL(declaringClass, isInterface, name, methodDescriptor) => {
            calledClass = declaringClass
            calledMethod = name
            calledMethodDescriptor = methodDescriptor
          }
          case INVOKESTATIC(declaringClass, isInterface, methodName, methodDescriptor) => {
            calledClass = declaringClass
            calledMethod = methodName
            calledMethodDescriptor = methodDescriptor
          }
          case INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor) => {

            // Sometimes a conversion from ReferenceType to ObjectType is not possible.
            // Therefore, we catch the occurring ClassCastException.
            try {
              val declaringClassAsObjectType = declaringClass.asObjectType
              // TODO filter object

              calledClass = declaringClassAsObjectType
              calledMethod = methodName
              calledMethodDescriptor = methodDescriptor
            } catch {
              case ccex: ClassCastException =>
            }

          }
          case _ => { /* do nothing */ }
        }

        /*
         * Check if:
         *    1. the three parameters calledClass and calledMethod and calledMethodDescriptor are set
         *    2. the called class is of interest
         */
        if (calledClass != null
          && calledMethod != null
          && calledMethodDescriptor != null //&& ClassFilter.apply(calledClass.fqn)) {
          ) { // TODO FILTER! && ClassFilter.filterCallingType(currentMethod, pc, project, calledClass.fqn)) {

          val callingTypeCheck = ClassFilter.findCallingType(analysisResult.analyzedMethod, pc, project)
          val callingTypes = if (callingTypeCheck.isEmpty) Set((calledClass, -1, "")) else callingTypeCheck

          callingTypes.filter(ct => ct._3.nonEmpty).foreach(ct => if (map.contains(ct._3)) map(ct._3))

          // determine an ID that identifies a method inside a class
          // ##################################################################
          val parameter = calledMethodDescriptor.valueToString.split(":") // contains the parameters and the return type

          // ID: return type; method name; (parameter types)
          val calledMethodID = parameter(1).trim() + " " + calledMethod + parameter(0)

          // check if the called method is part of a class with subtypes
          // ################################################################
          for (callingType <- callingTypes) {
            if (hasSubtypes(callingType._1)) {
              val methods = getCalledMethodsOfSubclassTypes(callingType._1, calledMethodID)

              if (methods != null) {
                // add capabilities of subtype methods to super method
                for ((method, subclassType) <- methods) {
                  if (capMap.contains(method)) {
                    val capMapEntry = capMap.get(method)
                    val newMethodCapsSet: HashSet[Capability] = new HashSet[Capability]
                    val newMethodCaps = capMapEntry.get

                    // create new hashmap of caps that contains the caps of the original hashmap
                    for (cap <- analysisResult.capabilities) {
                      newMethodCapsSet.add(cap)
                    }

                    // add new caps
                    for (cap <- newMethodCaps.toList) {
                      // TODO do not add the found caps but create an additional entry
                      // that includes this found caps
                      newMethodCapsSet.add(cap)

                      // TODO store subtype description
                    }

                    CapabilitiesSubtypeAnalysis.+=(new AnalysisResult(analysisResult.analyzedMethod, newMethodCapsSet.toList, 
                        subclassType, callingType._2, callingType._3, 
                        if(callingType._3.nonEmpty && map.contains(callingType._3)) map(callingType._3) else Set()))
                  }
                }
              }
            }
          }
        }
      }
    }
    return CapabilitiesSubtypeAnalysis
  }

  /**
   * Determines if a given class has subtypes
   */
  def hasSubtypes(declaringClass: ObjectType): Boolean = {
    val subclasses = project.classHierarchy.allSubclassTypes(declaringClass, false)

    if (subclasses.isEmpty) {
      return false
    } else {
      return true
    }
  }
  /**
   * Identifies method objects of all called subclass type methods
   * and the corresponding subclass types.
   */
  def getCalledMethodsOfSubclassTypes(declaringClass: ObjectType, calledMethodID: String): (List[(Method, String)]) = {
    val subclasses = project.classHierarchy.allSubclassTypes(declaringClass, false)
    val methods = new ListBuffer[(Method, String)]

    for (subclassType <- subclasses) {
      // load class object of current subclass type
      val subclass = project.allClassFiles.find(classFile => classFile.thisType.fqn.equals(subclassType.fqn))
      // load method object of the called method
      val method = subclass.get.methods.find(method => {
        // determine parameters and return value
        val parameter = method.descriptor.valueToString.split(":")
        // determine unique identifier
        val currentMethodID = parameter(1).trim() + " " + method.name + parameter(0)
        // check if given method is the expected one
        currentMethodID.equals(calledMethodID)
      })

      if (method != null && !method.isEmpty) {
        // result value: (method object, class subtype as string)
        methods.+=((method.get, subclass.get.thisType.toJava))
      }

    }

    return methods.toList
  }
}
