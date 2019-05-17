/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.project.implementedModules
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.overrideImplement.makeActual
import org.jetbrains.kotlin.idea.core.overrideImplement.makeNotActual
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.hasInlineModifier
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier

sealed class CreateExpectedFix<D : KtNamedDeclaration>(
    declaration: D,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module,
    generateIt: KtPsiFactory.(Project, D) -> D?
) : AbstractCreateDeclarationFix<D>(declaration, commonModule, generateIt) {

    private val targetExpectedClassPointer = targetExpectedClass?.createSmartPointer()

    override fun getText() = "Create expected $elementType in common module ${module.name}"

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val targetExpectedClass = targetExpectedClassPointer?.element
        val expectedFile = targetExpectedClass?.containingKtFile ?: getOrCreateImplementationFile() ?: return
        doGenerate(project, editor, originalFile = file, targetFile = expectedFile, targetClass = targetExpectedClass)
    }

    override fun findExistingFileToCreateDeclaration(
        originalFile: KtFile,
        originalDeclaration: KtNamedDeclaration
    ): KtFile? {
        for (otherDeclaration in originalFile.declarations) {
            if (otherDeclaration === originalDeclaration) continue
            if (!otherDeclaration.hasActualModifier()) continue
            val expectedDeclaration = otherDeclaration.liftToExpected() ?: continue
            return expectedDeclaration.containingKtFile
        }
        return null
    }

    companion object : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val d = DiagnosticFactory.cast(diagnostic, Errors.ACTUAL_WITHOUT_EXPECT)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return emptyList()
            val compatibility = d.b
            // For function we allow it, because overloads are possible
            if (compatibility.isNotEmpty() && declaration !is KtFunction) return emptyList()

            val containingClass = declaration.containingClassOrObject
            val expectedContainingClass = containingClass?.liftToExpected() as? KtClassOrObject
            if (containingClass != null && expectedContainingClass == null) {
                // In this case fix should be invoked on containingClass
                return emptyList()
            }
            // If there is already an expected class, we suggest only for its module,
            // otherwise we suggest for all relevant expected modules
            val expectedModules = expectedContainingClass?.module?.let { listOf(it) }
                ?: declaration.module?.implementedModules
                ?: return emptyList()
            return when (declaration) {
                is KtClassOrObject -> expectedModules.map { CreateExpectedClassFix(declaration, expectedContainingClass, it) }
                is KtFunction -> expectedModules.map { CreateExpectedFunctionFix(declaration, expectedContainingClass, it) }
                is KtProperty, is KtParameter -> expectedModules.map { CreateExpectedPropertyFix(declaration, expectedContainingClass, it) }
                else -> emptyList()
            }
        }
    }
}

class CreateExpectedClassFix(
    klass: KtClassOrObject,
    outerExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtClassOrObject>(klass, outerExpectedClass, commonModule, block@{ project, element ->
    val originalCollection = element.collectDeclarations(false).filter(KtDeclaration::canAddActualModifier).toList()
    val collection = originalCollection.filterNot(KtDeclaration::isAlwaysActual)
    val selectedElements = when {
        ApplicationManager.getApplication().isUnitTestMode -> collection.filter { it.isEffectivelyActual(false) }
        collection.any(KtDeclaration::hasActualModifier) && collection.any { !it.hasActualModifier() } -> {
            chooseMembers(project, collection) ?: return@block null
        }
        else -> null
    }

    project.executeWriteCommand("Repair actual members") {
        repairActualModifiers(originalCollection, selectedElements)
    }

    generateClassOrObject(project, true, element, listOfNotNull(outerExpectedClass))
})

private fun KtDeclaration.canAddActualModifier() = when (this) {
    is KtEnumEntry -> false
    is KtParameter -> this.hasValOrVar()
    else -> true
}

/***
 * @return null if close without OK
 */
private fun chooseMembers(project: Project, collection: Collection<KtDeclaration>): List<KtDeclaration>? {
    val classMembers = collection.map { DescriptorMemberChooserObject(it, it.resolveToDescriptorIfAny()!!) }
    return MemberChooser(
        classMembers.toTypedArray(),
        true,
        true,
        project
    ).run {
        title = "Choose actual members"
        setCopyJavadocVisible(false)
        selectElements(classMembers.filter { (it.element as KtDeclaration).hasActualModifier() }.toTypedArray())
        show()
        if (!isOK) null else selectedElements?.map { it.element as KtDeclaration }.orEmpty()
    }
}

private fun KtClassOrObject.collectDeclarations(withSelf: Boolean = true): Sequence<KtDeclaration> {
    val thisSequence = if (withSelf) sequenceOf(this) else emptySequence()
    val primaryConstructorSequence = primaryConstructorParameters.asSequence() + primaryConstructor.let {
        if (it != null) sequenceOf(it) else emptySequence()
    }
    return thisSequence + primaryConstructorSequence + declarations.asSequence().flatMap {
        if (it is KtClassOrObject) it.collectDeclarations() else sequenceOf(it)
    }
}

private fun repairActualModifiers(
    originalElements: Collection<KtDeclaration>,
    // If null, all class declarations are actual
    selectedElements: Collection<KtDeclaration>?
) {
    if (selectedElements == null)
        for (original in originalElements) {
            original.recursivelyMakeActual()
        }
    else
        for (original in originalElements) {
            if (original.isAlwaysActual() || original in selectedElements)
                original.recursivelyMakeActual()
            else
                original.makeNotActual()
        }
}

private tailrec fun KtDeclaration.recursivelyMakeActual() {
    makeActual()
    containingClassOrObject?.takeUnless(KtDeclaration::hasActualModifier)?.recursivelyMakeActual()
}

private fun KtDeclaration.isAlwaysActual(): Boolean {
    val primaryConstructor = when (this) {
        is KtPrimaryConstructor -> this
        is KtParameter -> (parent as? KtParameterList)?.parent as? KtPrimaryConstructor
        else -> null
    } ?: return false

    return primaryConstructor.containingClass()?.let {
        it.isAnnotation() || it.hasInlineModifier()
    } ?: false
}

class CreateExpectedPropertyFix(
    property: KtNamedDeclaration,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtNamedDeclaration>(property, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, true, element, descriptor, targetExpectedClass) }
})

class CreateExpectedFunctionFix(
    function: KtFunction,
    targetExpectedClass: KtClassOrObject?,
    commonModule: Module
) : CreateExpectedFix<KtFunction>(function, targetExpectedClass, commonModule, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, true, element, descriptor, targetExpectedClass) }
})

