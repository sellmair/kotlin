/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.implicit

import org.jetbrains.kotlin.types.KotlinType

data class TypeSubstitution(val source: KotlinType, val target: KotlinType)
