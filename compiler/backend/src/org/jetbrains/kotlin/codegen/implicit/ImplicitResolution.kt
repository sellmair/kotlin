/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.implicit

import org.jetbrains.kotlin.codegen.implicit.ImplicitCandidate.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.model.ImplicitValueArgument
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

sealed class ImplicitResolution {
    object FindInLocalFunction : ImplicitResolution() {
        override fun resolve(lookingFor: ValueParameterDescriptor,
                             parameters: List<ValueParameterDescriptor>,
                             argument: ImplicitValueArgument,
                             substitutions: List<TypeSubstitution>): ImplicitCandidate? {
            val candidates = mutableListOf<ImplicitCandidate>()
            val newSubstitutions = java.util.ArrayList(substitutions)
            for (i in 0 until parameters.size) {
                val parameter = parameters[i]
                if (parameter.isImplicit) {
                    val result = isReplaceable(parameter.returnType!!, lookingFor.returnType!!, substitutions)
                    if (result.canBeReplaced) {
                        newSubstitutions.addAll(result.substitutions)
                        candidates.add(FunctionParameter(parameter, i, newSubstitutions))
                    }
                }
            }

            return when (candidates.size) {
                1 -> candidates[0]
                else -> null
            }
        }
    }

    object FindInPackage : ImplicitResolution() {
        override fun resolve(lookingFor: ValueParameterDescriptor,
                             parameters: List<ValueParameterDescriptor>,
                             argument: ImplicitValueArgument,
                             substitutions: List<TypeSubstitution>): ImplicitCandidate? {
            val scope = findPackageScopeFor(lookingFor)

            if (scope != null) {
                val result = getCompatibleClasses(lookingFor, scope, substitutions)
                return when (result.candidates.size) {
                    1 -> SingleClassCandidate(result.candidates[0], result.substitutions)
                    else -> null
                }
            }

            return null
        }
    }

    object FindInTypeCompanion : ImplicitResolution() {
        override fun resolve(lookingFor: ValueParameterDescriptor,
                             parameters: List<ValueParameterDescriptor>,
                             argument: ImplicitValueArgument,
                             substitutions: List<TypeSubstitution>): ImplicitCandidate? {
            val arguments = lookingFor.returnType!!.arguments

            for (projection in arguments) {
                val companion = findCompanionFor(projection.type)

                if (companion != null) {
                    val result = getCompatibleClasses(lookingFor, companion.unsubstitutedMemberScope, substitutions)

                    return when (result.candidates.size) {
                        1 -> SingleClassCandidate(result.candidates[0], result.substitutions)
                        else -> null
                    }
                }
            }

            return null
        }
    }

    object FindInTypeclassCompanion : ImplicitResolution() {
        override fun resolve(lookingFor: ValueParameterDescriptor,
                             parameters: List<ValueParameterDescriptor>,
                             argument: ImplicitValueArgument,
                             substitutions: List<TypeSubstitution>): ImplicitCandidate? {
            val companion = findCompanionFor(argument.parameterDescriptor.returnType!!)

            if (companion != null) {
                val result = getCompatibleClasses(lookingFor, companion.unsubstitutedMemberScope, substitutions)

                return when (result.candidates.size) {
                    1 -> SingleClassCandidate(result.candidates[0], result.substitutions)
                    else -> null
                }
            }

            return null
        }
    }

    abstract fun resolve(lookingFor: ValueParameterDescriptor,
                         parameters: List<ValueParameterDescriptor>,
                         argument: ImplicitValueArgument,
                         substitutions: List<TypeSubstitution>): ImplicitCandidate?

    internal fun findCompanionFor(type: KotlinType): ClassDescriptor? {
        val scope = type.memberScope
        if (scope != null) {
            val descriptor = scope.getContributedClassifier(Name.identifier("Companion"), NoLookupLocation.FOR_DEFAULT_IMPORTS) as ClassDescriptor?
            if (descriptor != null) {
                return descriptor.original
            }
        }
        return null
    }

    internal fun findPackageScopeFor(lookingFor: ValueParameterDescriptor): LazyPackageMemberScope? {
        var descriptor: DeclarationDescriptor? = lookingFor.containingDeclaration
        while (descriptor !is LazyPackageDescriptor) {
            descriptor = descriptor!!.containingDeclaration
        }

        if (descriptor != null) {
            val packageDescriptor = descriptor as LazyPackageDescriptor?
            return packageDescriptor!!.getMemberScope() as LazyPackageMemberScope
        }
        return null
    }

    internal fun getCompatibleClasses(
            lookingFor: ValueParameterDescriptor,
            scope: MemberScope,
            substitutions: List<TypeSubstitution>
    ): CompatibilityResult {
        val declarations = scope.getContributedDescriptors(DescriptorKindFilter.ALL) { name -> true }
        val candidates = java.util.ArrayList<ClassDescriptor>()
        val newSubstitutions = java.util.ArrayList(substitutions)

        for (descriptor in declarations) {
            if (descriptor is ClassDescriptor) {
                if (descriptor.isExtension) {
                    val result = isCompatible(descriptor.defaultType, lookingFor.returnType!!, newSubstitutions)
                    if (result.canBeReplaced) {
                        candidates.add(descriptor)
                        newSubstitutions.addAll(result.substitutions)
                    }
                }
            }
        }
        return CompatibilityResult(candidates, newSubstitutions)
    }

    private fun isCompatible(candidate: KotlinType, type: KotlinType, substitutions: List<TypeSubstitution>): SubstitutionResult {
        val supertypes = candidate.constructor.supertypes
        val newSubstitutions = java.util.ArrayList(substitutions)
        for (supertype in supertypes) {
            val result = isReplaceable(supertype, type, newSubstitutions)
            if (!result.canBeReplaced) {
                return SubstitutionResult(false, substitutions)
            } else {
                newSubstitutions.addAll(result.substitutions)
            }
        }
        return SubstitutionResult(true, newSubstitutions)
    }

    internal fun isReplaceable(candidate: KotlinType, target: KotlinType, substitutions: List<TypeSubstitution>): SubstitutionResult {
        var newSubstitutions = java.util.ArrayList(substitutions)
        if (candidate.memberScope is LazyScopeAdapter) {
            if (target.memberScope !is LazyScopeAdapter) {
                val substitution = findSubstitution(candidate, substitutions)
                if (substitution == null) {
                    newSubstitutions.add(TypeSubstitution(candidate, target))
                    return SubstitutionResult(true, newSubstitutions)
                } else {
                    return isReplaceable(substitution, target, substitutions)
                }
            } else {
                newSubstitutions.add(TypeSubstitution(candidate, target))
                return SubstitutionResult(true, newSubstitutions)
            }
        }

        if (areEquivalentNames(candidate, target, substitutions)) {
            if (candidate.arguments.size == target.arguments.size) {
                for (i in 0 until candidate.arguments.size) {
                    val supertypeArgument = candidate.arguments[i]
                    val typeArgument = target.arguments[i]
                    val result = isReplaceable(supertypeArgument.type, typeArgument.type, newSubstitutions)
                    if (!result.canBeReplaced) {
                        return SubstitutionResult(false, substitutions)
                    } else {
                        newSubstitutions.addAll(result.substitutions)
                    }
                }
                return SubstitutionResult(true, newSubstitutions)
            }
        }

        return SubstitutionResult(false, substitutions)
    }

    private fun areEquivalentNames(candidate: KotlinType, target: KotlinType, substitutions: List<TypeSubstitution>): Boolean {
        if (candidate.constructor.toString().equals(target.constructor.toString())) {
            return true
        }
        if ((findSubstitution(candidate, substitutions)?.constructor?.toString() ?: "").equals(target.constructor.toString())) {
            return true
        }
        if ((findSubstitution(target, substitutions)?.constructor?.toString() ?: "").equals(candidate.constructor.toString())) {
            return true
        }

        return false
    }

    private fun findSubstitution(candidate: KotlinType, substitutions: List<TypeSubstitution>): KotlinType? {
        return substitutions
                .firstOrNull { it.source.equals(candidate) }
                ?.target
    }
}
