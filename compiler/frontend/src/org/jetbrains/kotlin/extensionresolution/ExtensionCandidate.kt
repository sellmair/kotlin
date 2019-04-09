/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensionresolution

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor

sealed class ExtensionCandidate(open val value: DeclarationDescriptor, open val substitutions: List<TypeSubstitution>) {
    data class FunctionParameter(
        override val value: ValueParameterDescriptor,
        val signatureIndex: Int,
        override val substitutions: List<TypeSubstitution>
    ) : ExtensionCandidate(value, substitutions)

    data class SingleClassCandidate(
        override val value: ClassDescriptor,
        override val substitutions: List<TypeSubstitution>
    ) : ExtensionCandidate(value, substitutions)

    data class NestedClassCandidate(
        override val value: ClassDescriptor,
        val parameters: List<ExtensionCandidate>,
        override val substitutions: List<TypeSubstitution>
    ) : ExtensionCandidate(value, substitutions)
}
