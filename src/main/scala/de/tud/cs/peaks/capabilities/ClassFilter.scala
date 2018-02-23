package de.tud.cs.peaks.capabilities

import java.net.URL

import org.opalj.ai.{ BaseAI, CorrelationalDomain, PC, domain }
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.{ ANEWARRAY, FieldReadAccess, Instruction, MULTIANEWARRAY, MethodInvocationInstruction, NEW }
import org.opalj.br.{ Method, ObjectType }
import org.opalj.collection.immutable.IntArraySet

object ClassFilter {

  /**
   * A list of classes that are not considered by the subtype analysis
   */
  val filteredClasses: List[String] = List("java/lang/Object", "java/lang/Iterable")

  def filter(className: String): Boolean = {
    if (filteredClasses.contains(className)) {
      return false
    } else {
      true
    }
  }

  /**
   *
   */
  def filterCallingType(method: Method, pc: PC, theProject: Project[URL], className: String): Boolean = {
    val callingTypes = findCallingType(method, pc, theProject)
    // TODO callingTypes.isEmpty
    !callingTypes.exists(tp => filteredClasses.contains(tp._1.fqn))
  }

  /**
   * Determines the class super type of a called method
   *
   * @param method Method object of the method that performs an invocation.
   * @param pc Program counter of the invocation instruction.
   * @param theProject The analysed project.
   *
   * @return A set of object types.
   *
   * @author Leonid
   */
  def findCallingType(method: Method, pc: PC, theProject: Project[URL]): Set[(ObjectType, Int, String)] = {
    var set = scala.collection.mutable.Set.empty[(ObjectType, Int, String)]

    if (method.body.nonEmpty) {
      val ai = BaseAI(method, new AnalysisDomain(theProject, method))
      if (ai != null && ai.domain != null && ai.domain.wasExecuted(pc) && ai.domain.operandOrigin(pc, 0) != null) {
        val defSites: IntArraySet = ai.domain.operandOrigin(pc, 0)
        val params = defSites.withFilter(pc => pc < 0)
        val insts = defSites.withFilter(pc => pc >= 0)

        for (param <- params) {
          if (method.parameterTypes.size > -param && method.parameterTypes(-param).isObjectType) {
            set += ((method.parameterTypes(-param).asObjectType, method.parameterTypes.indexOf(method.parameterTypes(-param)), ""))
          }
        }
        val association: Map[PC, Instruction] = method.body.get.associateWithIndex().toMap
        for (inst <- insts) {
          association(inst) match {
            case f: FieldReadAccess if f.fieldType.isObjectType => set += ((f.fieldType.asObjectType, -1, f.name))
//            case n: NEW => set += n.objectType
//            case n: ANEWARRAY if n.componentType.isObjectType => set += n.componentType.asObjectType
//            case n: MULTIANEWARRAY if n.arrayType.componentType.isObjectType => set += n.arrayType.componentType.asObjectType
//            case i: MethodInvocationInstruction if i.methodDescriptor.returnType.isObjectType => set += i.methodDescriptor.returnType.asObjectType
            case _ =>
          }
        }
        set.toSet
      } else
        set.toSet
    } else set.toSet
  }

  class AnalysisDomain(val project: Project[URL], val method: Method)
    extends CorrelationalDomain
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with domain.ThrowAllPotentialExceptionsConfiguration
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l0.TypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.l1.DefaultReferenceValuesBinding
    with domain.l1.DefaultIntegerRangeValues
    with domain.l1.DefaultLongValues
    with domain.l1.ConcretePrimitiveValuesConversions
    with domain.l1.LongValuesShiftOperators
    with domain.TheProject
    with domain.TheMethod
    with domain.RecordDefUse

}