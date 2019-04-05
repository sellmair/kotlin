/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.implicit.ExtensionCandidate
import org.jetbrains.kotlin.implicit.ExtensionCandidate.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

fun ExtensionCandidate.generate(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter) {
    when (this) {
        is FunctionParameter -> adapter.load(signatureIndex, typeMapper.mapType(value))
        is NestedClassCandidate -> generateInstantiation(lookingFor, typeMapper, adapter, value, parameters)
        is SingleClassCandidate -> if (value.kind == ClassKind.OBJECT) {
            adapter.getstatic(typeMapper.mapType(value).internalName, "INSTANCE", typeMapper.mapType(value).toString())
            adapter.checkcast(typeMapper.mapType(lookingFor))
        } else {
            generateInstantiation(lookingFor, typeMapper, adapter, value, emptyList())
        }
    }
}

fun ExtensionCandidate.generate(
    i: Int,
    lookingFor: ValueParameterDescriptor,
    callGenerator: CallGenerator,
    typeMapper: KotlinTypeMapper
) {
    val value = StackValue.operation(
        typeMapper.mapType(lookingFor),
        lookingFor.returnType
    ) { adapter ->
        generate(lookingFor, typeMapper, adapter)
    }
    callGenerator.putCapturedValueOnStack(value, typeMapper.mapType(lookingFor), i)
}

private fun generateInstantiation(
    lookingFor: ValueParameterDescriptor,
    typeMapper: KotlinTypeMapper,
    adapter: InstructionAdapter,
    value: ClassDescriptor,
    parameters: List<ExtensionCandidate>
) {
    adapter.anew(typeMapper.mapType(value))
    adapter.dup()
    for (parameter in parameters) {
        parameter.generate(lookingFor, typeMapper, adapter)
    }
    adapter.invokespecial(
        typeMapper.mapType(value).internalName,
        "<init>", generateConstructorSignature(value, typeMapper), false
    )
    adapter.checkcast(typeMapper.mapType(lookingFor))
}

private fun generateConstructorSignature(candidate: ClassDescriptor, typeMapper: KotlinTypeMapper): String {
    val scope = candidate.unsubstitutedPrimaryConstructor
    val signature = (scope?.valueParameters ?: emptyList()).map { typeMapper.mapType(it.returnType!!).toString() }.joinToString()
    return "($signature)V"
}
