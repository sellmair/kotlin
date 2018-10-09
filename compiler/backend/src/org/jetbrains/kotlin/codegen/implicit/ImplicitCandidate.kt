/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.implicit

import org.jetbrains.kotlin.codegen.CallGenerator
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

sealed class ImplicitCandidate(open val substitutions: List<TypeSubstitution>) {
    data class FunctionParameter(val value: ValueParameterDescriptor,
                                 val signatureIndex: Int,
                                 override val substitutions: List<TypeSubstitution>) : ImplicitCandidate(substitutions) {
        override fun generate(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter) {
            adapter.load(signatureIndex, typeMapper.mapType(value))
        }
    }

    data class SingleClassCandidate(val value: ClassDescriptor,
                                    override val substitutions: List<TypeSubstitution>) : ImplicitCandidate(substitutions) {
        override fun generate(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter) {
            if (value.kind == ClassKind.OBJECT) {
                adapter.getstatic(typeMapper.mapType(value).internalName, "INSTANCE", typeMapper.mapType(value).toString())
                adapter.checkcast(typeMapper.mapType(lookingFor))
            } else {
                generateInstantiation(lookingFor, typeMapper, adapter, value, emptyList())
            }
        }
    }

    data class NestedClassCandidate(val value: ClassDescriptor,
                                    val parameters: List<ImplicitCandidate>,
                                    override val substitutions: List<TypeSubstitution>) : ImplicitCandidate(substitutions) {
        override fun generate(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter) {
            generateInstantiation(lookingFor, typeMapper, adapter, value, parameters)
        }
    }

    fun generate(i: Int, lookingFor: ValueParameterDescriptor, callGenerator: CallGenerator, typeMapper: KotlinTypeMapper) {
        val value = StackValue.operation(typeMapper.mapType(lookingFor),
                                         lookingFor.returnType) { adapter ->
            generate(lookingFor, typeMapper, adapter)
        }
        callGenerator.putCapturedValueOnStack(value, typeMapper.mapType(lookingFor), i)
    }

    abstract fun generate(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter)

    fun generateInstantiation(lookingFor: ValueParameterDescriptor, typeMapper: KotlinTypeMapper, adapter: InstructionAdapter, value: ClassDescriptor, parameters: List<ImplicitCandidate>) {
        adapter.anew(typeMapper.mapType(value))
        adapter.dup()
        for (parameter in parameters) {
            parameter.generate(lookingFor, typeMapper, adapter)
        }
        adapter.invokespecial(typeMapper.mapType(value).getInternalName(),
                              "<init>", generateConstructorSignature(value, typeMapper), false)
        adapter.checkcast(typeMapper.mapType(lookingFor))
    }

    fun generateConstructorSignature(candidate: ClassDescriptor, typeMapper: KotlinTypeMapper): String {
        val scope = candidate.unsubstitutedPrimaryConstructor
        val signature = (scope?.valueParameters ?: emptyList()).map { typeMapper.mapType(it.returnType!!).toString() }.joinToString()
        return "($signature)V"
    }
}
