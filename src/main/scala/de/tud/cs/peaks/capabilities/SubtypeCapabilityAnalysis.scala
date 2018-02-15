package de.tud.cs.peaks.capabilities

import org.opalj.br.analyses.Project
import org.opalj.br.Method
import java.net.URL
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.ObjectType
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.MethodDescriptor
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

/**
 * Performs a subtype analysis on the
 * results of the capability analysis
 */
class SubtypeCapabilityAnalysis(project: Project[URL], capMap: HashMap[Method, HashSet[Capability]], capabilityAnalysisResults: Iterable[(org.opalj.br.Method, scala.collection.mutable.HashSet[de.tud.cs.peaks.capabilities.Capability], String)]) {

  var CapabilitiesSubtypeAnalysis = ListBuffer.empty[(Method, HashSet[Capability], String)]
  // hashtable attributes
  // [KEY         , VALUE                     ]
  // [called class, (called methods, subtypes)]
  var subtypes = new java.util.Hashtable[(String, String), (List[Method], List[String])] //[called class, (called methods, subtypes)]

  /**
   * Method to start the capability analysis for known subtypes.
   */
  def startAnalysis(): List[(Method, HashSet[Capability], String)] = {
    // loop over the found capability analysis results
    var i = 0
    var max = capabilityAnalysisResults.size

    for ((currentMethod, currentMethodCaps, subtype) <- capabilityAnalysisResults) {
      var listOfSubtypes = ListBuffer.empty[String]
      i = i + 1
      printf(i + "/" + max + "\n")
 
      //currentMethod.parameterTypes.toSet ++ currentMethod.body.get.instructions.filter(p)
      // loop over method instructions
      for ((pc, instruction) <- currentMethod.body.get.associateWithIndex()) {
        var calledClass: ObjectType = null
        var calledMethod: String = null //MethodDescriptor = null
        var calledMethodDescriptor: MethodDescriptor = null

        instruction match {
          //case INVOKEDYNAMIC(bootstrapMethod, name, methodDescriptor) => {
          // no declaring class
          // TODO handle INVOKEDYNAMIC instruction
          //}
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

        // Check if the two parameters calledClass and calledMethod are set
        if (calledClass != null && calledMethod != null && calledMethodDescriptor != null) {

          // determine an ID that identifies a method inside a class
          // ##################################################################
          val parameter = calledMethodDescriptor.valueToString.split(":") // contains the parameters and the return type

          // ID: return type; method name; (parameter types)
          val calledMethodID = parameter(1).trim() + " " + calledMethod + parameter(0)

          // check if the called method is part of a class with subtypes
          // ################################################################
          if (hasSubtypes(calledClass)) {
            val methods = getCalledMethodsOfSubclassTypes(calledClass, calledMethodID)

            if (methods != null) {
              // add capabilities of subtype methods to super method
              for ((method, subclassType) <- methods) {

                //                val capMapEntry = capMap.find(capMapEntry => {
                //                  if (capMapEntry._1 == None || method == None) {
                //                    false
                //                  } else {
                //                    capMapEntry._1.toJava.equals(method.toJava)
                //                  }
                //                })

                if (capMap.contains(method)) {
                  val capMapEntry = capMap.get(method)

                  val newMethodCapsSet: HashSet[Capability] = new HashSet[Capability]

                  //                if (!capMapEntry.isEmpty) {
                  val newMethodCaps = capMapEntry.get

                  // create new hashmap of caps that contains the caps of the original hashmap
                  for (cap <- currentMethodCaps) {
                    newMethodCapsSet.add(cap)
                  }

                  // add new caps
                  for (cap <- newMethodCaps.toList) {
                    // TODO do not add the found caps but create an additional entry
                    // that includes this found caps
                    newMethodCapsSet.add(cap)

                    // TODO store subtype description
                  }

                  CapabilitiesSubtypeAnalysis.+=((currentMethod, newMethodCapsSet, subclassType))
                }

                //                CapabilitiesSubtypeAnalysis.+=((currentMethod, newMethodCapsSet, subclassType))
              }

              // add subtypes to the list of subtypes without duplicates
              //for (subtype <- methods._2.toList) {
              //  if (!listOfSubtypes.contains(subtype)) {
              //    listOfSubtypes.+=(subtype)
              //  }
              //}

            }
            //}
          }
        }
      }
      //CapabilitiesSubtypeAnalysis.+=((currentMethod, currentMethodCaps, listOfSubtypes.toList))

    }

    return CapabilitiesSubtypeAnalysis.toList
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