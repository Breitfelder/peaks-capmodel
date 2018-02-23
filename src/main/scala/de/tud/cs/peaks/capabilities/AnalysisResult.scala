package de.tud.cs.peaks.capabilities

import org.opalj.br.Method

class AnalysisResult(val analyzedMethod:Method, val capabilities:List[Capability],
    val subType:String, val paramIndex:Int, val name:String, val constructorParams:Set[(Method,Int)])