package graphql.validation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

import graphql.Internal;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;

@Internal
@SuppressWarnings("rawtypes")
public class RulesVisitor implements DocumentVisitor {
    private final ValidationContext validationContext;
    private final Stack<Scope> scopes;

    private class Scope {
        private final boolean root;
        private final Node node;
        private final Scope parent;
        private final ImmutableSet<AbstractRule> rulesInScope;
        private final ImmutableSet<AbstractRule> checked;
        private final ImmutableSet<AbstractRule> suppressed;

        Scope(ImmutableSet<AbstractRule> rulesInScope, boolean root) {
            this(
                rulesInScope,
                ImmutableSet.of(),
                ImmutableSet.of(),
                null,
                null,
                root
            );
        }

        private Scope(
            ImmutableSet<AbstractRule> rulesInScope,
            ImmutableSet<AbstractRule> checked,
            ImmutableSet<AbstractRule> suppressed,
            Node node,
            Scope parent,
            boolean root
        ) {
            this.rulesInScope = rulesInScope;
            this.checked = checked;
            this.suppressed = suppressed;
            this.node = node;
            this.parent = parent;
            this.root = root;
        }

        Scope withChecked(ImmutableSet<AbstractRule> checked) {
            return new Scope(rulesInScope, checked, suppressed, node, parent, root);
        }

        Scope withSuppressed(ImmutableSet<AbstractRule> suppressed) {
            Iterator<AbstractRule> itr = Iterators.concat(suppressed.iterator(), this.suppressed.iterator());
            ImmutableSet<AbstractRule> mergedSuppressed = ImmutableSet.copyOf(itr);
            return new Scope(rulesInScope, checked, mergedSuppressed, node, parent, root);
        }

        Scope withRulesInScope(ImmutableSet<AbstractRule> rulesInScope) {
            return new Scope(rulesInScope, checked, suppressed, node, parent, root);
        }

        Scope asRoot(boolean root) {
            return new Scope(rulesInScope, checked, suppressed, node, parent, root);
        }

        Scope child(Node node) {
            return new Scope(rulesInScope, ImmutableSet.of(), suppressed, node, this, root);
        }

        ImmutableSet<AbstractRule> uncheckedRulesForNode() {
            HashSet<AbstractRule> unchecked = new HashSet<>(rulesInScope);
            unchecked.removeAll(suppressed);
            for (Scope scope = this; scope != null && !unchecked.isEmpty() && scope.root == this.root; scope = scope.parent) {
                if (scope.node == node) {
                    unchecked.removeAll(scope.checked);
                }
            }
            return ImmutableSet.copyOf(unchecked);
        }
    }

    public RulesVisitor(ValidationContext validationContext, List<AbstractRule> rules) {
        this.validationContext = validationContext;
        this.scopes = new Stack<>();
        this.scopes.push(new Scope(ImmutableSet.copyOf(rules), true));
    }

    @Override
    public void enter(Node node, List<Node> ancestors) {
        validationContext.getTraversalContext().enter(node, ancestors);
        Scope scope = this.scopes.peek().child(node);
        this.scopes.push(scope);

        Scope newScope;
        if (node instanceof Document){
            newScope = checkDocument((Document) node, scope);
        } else if (node instanceof Argument) {
            newScope = checkArgument((Argument) node, scope);
        } else if (node instanceof TypeName) {
            newScope = checkTypeName((TypeName) node, scope);
        } else if (node instanceof VariableDefinition) {
            newScope = checkVariableDefinition((VariableDefinition) node, scope);
        } else if (node instanceof Field) {
            newScope = checkField((Field) node, scope);
        } else if (node instanceof InlineFragment) {
            newScope = checkInlineFragment((InlineFragment) node, scope);
        } else if (node instanceof Directive) {
            newScope = checkDirective((Directive) node, ancestors, scope);
        } else if (node instanceof FragmentSpread) {
            newScope = checkFragmentSpread((FragmentSpread) node, ancestors, scope);
        } else if (node instanceof FragmentDefinition) {
            newScope = checkFragmentDefinition((FragmentDefinition) node, scope);
        } else if (node instanceof OperationDefinition) {
            newScope = checkOperationDefinition((OperationDefinition) node, scope);
        } else if (node instanceof VariableReference) {
            newScope = checkVariable((VariableReference) node, scope);
        } else if (node instanceof SelectionSet) {
            newScope = checkSelectionSet((SelectionSet) node, scope);
        } else {
            newScope = scope;
        }

        // replace the top scope in the stack with the modified scope returned by the checker
        scopes.pop();
        scopes.push(newScope);
    }

    private Scope checkDocument(Document node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkDocument(node));
        return scope.withChecked(rules);
    }

    private Scope checkArgument(Argument node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkArgument(node));
        return scope.withChecked(rules);
    }

    private Scope checkTypeName(TypeName node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkTypeName(node));
        return scope.withChecked(rules);
    }

    private Scope checkVariableDefinition(VariableDefinition node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkVariableDefinition(node));
        return scope.withChecked(rules);
    }

    private Scope checkField(Field node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkField(node));
        return scope.withChecked(rules);
    }

    private Scope checkInlineFragment(InlineFragment node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkInlineFragment(node));
        return scope.withChecked(rules);
    }

    private Scope checkDirective(Directive node, List<Node> ancestors, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkDirective(node, ancestors));
        return scope.withChecked(rules);
    }

    private Scope checkFragmentSpread(FragmentSpread node, List<Node> ancestors, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkFragmentSpread(node));

        ImmutableSet<AbstractRule> rulesVisitingFragmentSpreads = filterVisitFragmentSpreads(rules, true);
        if (rulesVisitingFragmentSpreads.size() > 0) {
            FragmentDefinition fragment = validationContext.getFragment(node.getName());
            if (fragment != null && !ancestors.contains(fragment)) {
                Scope newScope = scope.child(fragment)
                    .asRoot(false)
                    .withRulesInScope(rulesVisitingFragmentSpreads);
                scopes.push(newScope);
                new LanguageTraversal(ancestors).traverse(fragment, this);
                return scopes.pop();
            }
        }

        return scope.withChecked(rules);
    }

    private ImmutableSet<AbstractRule> filterVisitFragmentSpreads(Set<AbstractRule> rules, boolean isVisitFragmentSpreads) {
        Iterator<AbstractRule> itr = rules.stream()
            .filter(r -> r.isVisitFragmentSpreads() == isVisitFragmentSpreads)
            .iterator();
        return ImmutableSet.copyOf(itr);
    }

    private Scope checkFragmentDefinition(FragmentDefinition node, Scope scope) {
        if (scope.root) {
            ImmutableSet<AbstractRule> uncheckedRulesForNode = scope.uncheckedRulesForNode();
            ImmutableSet<AbstractRule> checkRules = filterVisitFragmentSpreads(uncheckedRulesForNode, false);
            ImmutableSet<AbstractRule> suppressRules = filterVisitFragmentSpreads(uncheckedRulesForNode, true);
            checkRules.forEach(r -> r.checkFragmentDefinition(node));

            // Suppress rules with isVisitFragmentSpreads in this subtree
            // expect that these rules will be covered by checkFragmentSpread
            return scope.withChecked(uncheckedRulesForNode).withSuppressed(suppressRules);
        } else {
            ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
            rules.forEach(r -> r.checkFragmentDefinition(node));
            return scope.withChecked(rules);
        }
    }

    private Scope checkOperationDefinition(OperationDefinition node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkOperationDefinition(node));
        return scope.withChecked(rules);
    }

    private Scope checkSelectionSet(SelectionSet node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkSelectionSet(node));
        return scope.withChecked(rules);
    }

    private Scope checkVariable(VariableReference node, Scope scope) {
        ImmutableSet<AbstractRule> rules = scope.uncheckedRulesForNode();
        rules.forEach(r -> r.checkVariable(node));
        return scope.withChecked(rules);
    }

    @Override
    public void leave(Node node, List<Node> ancestors) {
        validationContext.getTraversalContext().leave(node, ancestors);
        Scope scope = scopes.pop();

        if (node instanceof Document) {
            documentFinished((Document) node, scope);
        } else if (node instanceof OperationDefinition) {
            leaveOperationDefinition((OperationDefinition) node, scope);
        } else if (node instanceof SelectionSet) {
            leaveSelectionSet((SelectionSet) node, scope);
        }
    }

    private void leaveSelectionSet(SelectionSet node, Scope scope) {
        scope.rulesInScope.forEach(r -> r.leaveSelectionSet(node));
    }

    private void leaveOperationDefinition(OperationDefinition node, Scope scope) {
        scope.rulesInScope.forEach(r -> r.leaveOperationDefinition(node));
    }

    private void documentFinished(Document node, Scope scope) {
        scope.rulesInScope.forEach(r -> r.documentFinished(node));
    }
}
