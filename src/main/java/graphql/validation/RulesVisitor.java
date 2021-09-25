package graphql.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableList;

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
    private final Stack<Context> contextStack;

    private static class Context {
        private final Set<Node> seen;
        private final ImmutableList<AbstractRule> rules;
        private final boolean root;

        Context(ImmutableList<AbstractRule> rules) {
            this(rules, new HashSet<>(), false);
        }

        private Context(ImmutableList<AbstractRule> rules, Set<Node> seen, boolean root) {
            this.rules = rules;
            this.seen = seen;
            this.root = root;
        }

        void markSeen(Node node) {
            seen.add(node);
        }

        boolean hasSeen(Node node) {
            return seen.contains(node);
        }

        Context withRules(ImmutableList<AbstractRule> rules) {
            return new Context(rules, this.seen, this.root);
        }

        Context asRoot(boolean root) {
            return new Context(this.rules, this.seen, root);
        }

        Context resetSeen() {
            return new Context(this.rules, new HashSet<>(), root);
        }
    }

    public RulesVisitor(ValidationContext validationContext, List<AbstractRule> rules) {
        this.validationContext = validationContext;
        this.contextStack = new Stack<>();

        Context ctx = new Context(ImmutableList.copyOf(rules)).asRoot(true);
        this.contextStack.push(ctx);
    }

    private boolean tryEnter(Node node, Context ctx) {
        // directives need to be revalidated in every location they occur in
        boolean canEnter;
        if (node instanceof Directive) canEnter = true;
        else canEnter = !ctx.hasSeen(node);

        if (canEnter) {
            ctx.markSeen(node);
        }
        return canEnter;
    }

    @Override
    public void enter(Node node, List<Node> ancestors) {
        validationContext.getTraversalContext().enter(node, ancestors);
        Context ctx = contextStack.push(contextStack.peek());

        if (tryEnter(node, ctx)) {
            if (node instanceof Document){
                checkDocument((Document) node, ctx.rules);
            } else if (node instanceof Argument) {
                checkArgument((Argument) node, ctx.rules);
            } else if (node instanceof TypeName) {
                checkTypeName((TypeName) node, ctx.rules);
            } else if (node instanceof VariableDefinition) {
                checkVariableDefinition((VariableDefinition) node, ctx.rules);
            } else if (node instanceof Field) {
                checkField((Field) node, ctx.rules);
            } else if (node instanceof InlineFragment) {
                checkInlineFragment((InlineFragment) node, ctx.rules);
            } else if (node instanceof Directive) {
                checkDirective((Directive) node, ancestors, ctx.rules);
            } else if (node instanceof FragmentSpread) {
                checkFragmentSpread((FragmentSpread) node, ctx.rules, ancestors);
            } else if (node instanceof FragmentDefinition) {
                checkFragmentDefinition((FragmentDefinition) node, ctx.rules);
            } else if (node instanceof OperationDefinition) {
                checkOperationDefinition((OperationDefinition) node, ctx.rules);
            } else if (node instanceof VariableReference) {
                checkVariable((VariableReference) node, ctx.rules);
            } else if (node instanceof SelectionSet) {
                checkSelectionSet((SelectionSet) node, ctx.rules);
            }
        }
    }

    private void checkDocument(Document node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkDocument(node));
    }

    private void checkArgument(Argument node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkArgument(node));
    }

    private void checkTypeName(TypeName node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkTypeName(node));
    }

    private void checkVariableDefinition(VariableDefinition node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkVariableDefinition(node));
    }

    private void checkField(Field node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkField(node));
    }

    private void checkInlineFragment(InlineFragment node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkInlineFragment(node));
    }

    private void checkDirective(Directive node, List<Node> ancestors, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkDirective(node, ancestors));
    }

    private void checkFragmentSpread(FragmentSpread node, List<AbstractRule> rules, List<Node> ancestors) {
        rules.forEach(r -> r.checkFragmentSpread(node));

        ImmutableList<AbstractRule> rulesVisitingFragmentSpreads = filterVisitFragmentSpreads(rules, true);
        if (rulesVisitingFragmentSpreads.size() > 0) {
            FragmentDefinition fragment = validationContext.getFragment(node.getName());
            if (fragment != null && !ancestors.contains(fragment)) {
                Context newCtx = contextStack.peek()
                    .asRoot(false)
                    .resetSeen()
                    .withRules(rulesVisitingFragmentSpreads);
                contextStack.push(newCtx);
                new LanguageTraversal(ancestors).traverse(fragment, this);
                contextStack.pop();
            }
        }
    }

    private ImmutableList<AbstractRule> filterVisitFragmentSpreads(List<AbstractRule> rules, boolean isVisitFragmentSpreads) {
        List<AbstractRule> result = new ArrayList<>();
        rules.stream()
            .filter(r -> r.isVisitFragmentSpreads() == isVisitFragmentSpreads)
            .forEach(result::add);
        return ImmutableList.copyOf(result);
    }

    private void checkFragmentDefinition(FragmentDefinition node, List<AbstractRule> rules) {
        Context ctx = contextStack.peek();
        if (ctx.root) {
            // Replace the rules for this subtree to skip rules with isVisitFragmentSpreads
            // expect that these rules will be covered by checkFragmentSpread
            ImmutableList<AbstractRule> localRules = filterVisitFragmentSpreads(rules, false);
            contextStack.push(contextStack.pop().withRules(localRules));

            localRules.forEach(r -> r.checkFragmentDefinition(node));
        } else {
            rules.forEach(r -> r.checkFragmentDefinition(node));
        }
    }

    private void checkOperationDefinition(OperationDefinition node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkOperationDefinition(node));
    }

    private void checkSelectionSet(SelectionSet node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkSelectionSet(node));
    }

    private void checkVariable(VariableReference node, List<AbstractRule> rules) {
        rules.forEach(r -> r.checkVariable(node));
    }

    @Override
    public void leave(Node node, List<Node> ancestors) {
        validationContext.getTraversalContext().leave(node, ancestors);
        List<AbstractRule> rules = contextStack.peek().rules;

        if (node instanceof Document) {
            documentFinished((Document) node, rules);
        } else if (node instanceof OperationDefinition) {
            leaveOperationDefinition((OperationDefinition) node, rules);
        } else if (node instanceof SelectionSet) {
            leaveSelectionSet((SelectionSet) node, rules);
        }

        contextStack.pop();
    }

    private void leaveSelectionSet(SelectionSet node, List<AbstractRule> rules) {
        rules.forEach(r -> r.leaveSelectionSet(node));
    }

    private void leaveOperationDefinition(OperationDefinition node, List<AbstractRule> rules) {
        rules.forEach(r -> r.leaveOperationDefinition(node));
    }

    private void documentFinished(Document node, List<AbstractRule> rules) {
        rules.forEach(r -> r.documentFinished(node));
    }
}
