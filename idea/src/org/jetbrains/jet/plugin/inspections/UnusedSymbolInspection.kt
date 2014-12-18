/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.jet.lang.psi.JetVisitorVoid
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearch
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.jet.plugin.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.jet.plugin.findUsages.toClassHelper
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import com.intellij.util.Processor
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindClassUsagesHandler
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.plugin.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.jet.plugin.findUsages.toHelper
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lexer.JetTokens


public class UnusedSymbolInspection : AbstractKotlinInspection() {
    private val javaInspection = UnusedDeclarationInspection()

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            override fun visitClass(klass: JetClass) {
                if (klass.getName() == null) return

                if (isEntryPoint(klass)) return
                if (hasNonTrivialUsages(klass)) return
                if (classHasTextUsages(klass)) return

                holder.registerProblem(
                        klass.getNameIdentifier(),
                        JetBundle.message("unused.class", klass.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }

            override fun visitNamedFunction(function: JetNamedFunction) {
                if (function.getName() == null) return

                if (function.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return
                if (isEntryPoint(function)) return
                if (hasNonTrivialUsages(function)) return

                holder.registerProblem(
                        function.getNameIdentifier(),
                        JetBundle.message("unused.function", function.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }
        }
    }

    private fun isEntryPoint(declaration: JetNamedDeclaration): Boolean {
        val lightElement: PsiElement? = when (declaration) {
            is JetClass ->  LightClassUtil.getPsiClass(declaration)
            is JetNamedFunction -> LightClassUtil.getLightClassMethod(declaration)
            else -> null
        }
        return lightElement != null && javaInspection.isEntryPoint(lightElement)
    }

    private fun classHasTextUsages(klass: JetClass): Boolean {
        var hasTextUsages = false

        val classUseScope = klass.getUseScope()
        // Finding text usages
        if (classUseScope is GlobalSearchScope) {
            val findClassUsagesHandler = KotlinFindClassUsagesHandler(klass, KotlinFindUsagesHandlerFactory(klass.getProject()))
            findClassUsagesHandler.processUsagesInText(
                    klass,
                    { hasTextUsages = true; false },
                    classUseScope
            )
        }

        return hasTextUsages
    }

    private fun hasNonTrivialUsages(declaration: JetNamedDeclaration): Boolean {
        val project = declaration.getProject()

        val searchHelper = when (declaration) {
            is JetClass -> KotlinClassFindUsagesOptions(project).toClassHelper()
            is JetNamedFunction -> KotlinFunctionFindUsagesOptions(project).toHelper()
            else -> return false
        }

        val useScope = declaration.getUseScope()
        val request = searchHelper.newRequest(UsagesSearchTarget(declaration, useScope))
        val query = UsagesSearch.search(request)

        var foundNonTrivialUsage = false
        query.forEach(Processor {
            usage ->
            if (declaration.isAncestor(usage.getElement())) {
                true
            } else {
                foundNonTrivialUsage = true
                false
            }
        })

        return foundNonTrivialUsage
    }
}