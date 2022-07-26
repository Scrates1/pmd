/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast

import net.sourceforge.pmd.annotation.InternalApi
import net.sourceforge.pmd.lang.apex.ApexParserOptions
import net.sourceforge.pmd.lang.ast.ParseException
import net.sourceforge.pmd.lang.ast.SourceCodePositioner

import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.TypeRef
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.EnumDeclaration
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.TriggerDeclaration
import com.google.summit.ast.declaration.TypeDeclaration
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.ast.modifier.Modifier

@Deprecated("internal")
@InternalApi
@Suppress("DEPRECATION")
class ApexTreeBuilder(val sourceCode: String, val parserOptions: ApexParserOptions) {
    private val sourceCodePositioner = SourceCodePositioner(sourceCode)

    /** Builds and returns an [ApexNode] AST corresponding to the given [root] node. */
    fun buildTree(root: CompilationUnit): ApexRootNode<TypeDeclaration> =
        build(root, parent = null) as? ApexRootNode<TypeDeclaration>
            ?: throw ParseException("Unable to build tree")

    /**
     * Builds an [ApexNode] wrapper for [node].
     *
     * Sets the parent of the resulting [ApexNode] to [parent], if it's not `null`.
     */
    private fun build(node: Node?, parent: ApexNode<*>?): AbstractApexNode? {
        val wrapper: AbstractApexNode? =
            when (node) {
                null -> null
                is CompilationUnit -> build(node.typeDeclaration, parent)
                is TypeDeclaration -> buildTypeDeclaration(node)
                is Identifier,
                is KeywordModifier,
                is TypeRef -> null
                else -> {
                    println("No adapter exists for type ${node::class.qualifiedName}")
                    // TODO(b/239648780): temporary print
                    null
                }
            }

        wrapper?.setParent(parent)
        wrapper?.handleSourceCode(sourceCode)
        wrapper?.calculateLineNumbers(sourceCodePositioner)
        return wrapper
    }

    /** Calls [build] on each of [nodes]. */
    private fun build(nodes: List<Node>, parent: ApexNode<*>?) = nodes.forEach { build(it, parent) }

    /**
     * Calls [build] on each [child][Node.getChildren] of [node].
     *
     * If [exclude] is provided, child nodes matching this predicate are not visited.
     */
    private fun buildChildren(
        node: Node,
        parent: ApexNode<*>?,
        exclude: (Node) -> Boolean = { false } // exclude none by default
    ) = node.getChildren().filterNot(exclude).forEach { build(it, parent) }

    /** Builds an [ApexRootNode] wrapper for the [TypeDeclaration] node. */
    private fun buildTypeDeclaration(node: TypeDeclaration) =
        when (node) {
            is ClassDeclaration ->
                ASTUserClass(node).apply {
                    val modifiers = buildModifiers(node.modifiers)
                    modifiers.setParent(this)
                    buildChildren(node, parent = this, exclude = { it in node.modifiers })
                }
            is InterfaceDeclaration ->
                ASTUserInterface(node).apply {
                    val modifiers = buildModifiers(node.modifiers)
                    modifiers.setParent(this)
                    buildChildren(node, parent = this, exclude = { it in node.modifiers })
                }
            is EnumDeclaration -> ASTUserEnum(node) // TODO(b/239648780): enum body is untranslated
            is TriggerDeclaration -> ASTUserTrigger(node) // TODO(b/239648780): visit children
        }

    /** Builds an [ASTModifierNode] wrapper for the list of [Modifier]s. */
    private fun buildModifiers(modifiers: List<Modifier>) =
        ASTModifierNode(modifiers).apply { build(modifiers, parent = this) }

    /**
     * If [parent] is not null, adds this [ApexNode] as a [child][ApexNode.jjtAddChild] and sets
     * [parent] as the [parent][ApexNode.jjtSetParent].
     */
    private fun ApexNode<*>.setParent(parent: ApexNode<*>?) {
        if (parent != null) {
            parent.jjtAddChild(this, parent.numChildren)
            this.jjtSetParent(parent)
        }
    }

    val suppressMap
        get() = emptyMap<Int, String>()
    // TODO(b/239648780)
}
