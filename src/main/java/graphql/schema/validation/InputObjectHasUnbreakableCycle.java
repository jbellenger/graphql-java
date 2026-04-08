package graphql.schema.validation;

import graphql.Internal;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.LinkedHashSet;
import java.util.Set;

import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.schema.GraphQLTypeUtil.unwrapNonNullAs;
import static java.lang.String.format;

/**
 * Implements the spec's {@code InputObjectHasUnbreakableCycle} validation algorithm.
 * <p>
 * This unifies the old "no unbroken non-null input cycles" check with OneOf input
 * inhabitability into a single rule. An input object has an unbreakable cycle when
 * no finite value can be constructed for it:
 * <ul>
 *   <li>For regular input objects: any non-null field leading to an unbreakable cycle
 *       makes the type uninhabitable (all non-null fields must be satisfied).</li>
 *   <li>For OneOf input objects: all fields leading to unbreakable cycles makes the
 *       type uninhabitable (only one field must be provided, but if every option
 *       leads to a cycle, there is no escape).</li>
 *   <li>List types always break cycles (an empty list is a valid value).</li>
 *   <li>Scalar and enum types always break cycles.</li>
 * </ul>
 *
 * @see <a href="https://github.com/graphql/graphql-spec/pull/1211">graphql-spec PR #1211</a>
 */
@Internal
public class InputObjectHasUnbreakableCycle extends GraphQLTypeVisitorStub {

    private final Set<GraphQLInputObjectType> knownNoCycle = new LinkedHashSet<>();

    @Override
    public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType inputObjectType,
            TraverserContext<GraphQLSchemaElement> context) {
        if (hasUnbreakableCycle(inputObjectType, new LinkedHashSet<>())) {
            SchemaValidationErrorCollector errorCollector =
                    context.getVarFromParents(SchemaValidationErrorCollector.class);
            String message = format(
                    "Input Object %s forms an unbreakable cycle and cannot be constructed.",
                    inputObjectType.getName());
            errorCollector.addError(
                    new SchemaValidationError(SchemaValidationErrorType.UnbrokenInputCycle, message));
        }
        return TraversalControl.CONTINUE;
    }

    /**
     * Determines whether the given input object type has an unbreakable cycle,
     * meaning no finite value can ever be constructed for it.
     *
     * @param inputObject the input object type to check
     * @param visited     the set of input object types already visited in this traversal path
     * @return true if the type has an unbreakable cycle
     */
    private boolean hasUnbreakableCycle(GraphQLInputObjectType inputObject,
            Set<GraphQLInputObjectType> visited) {
        if (knownNoCycle.contains(inputObject)) {
            return false;
        }
        if (visited.contains(inputObject)) {
            return true;
        }

        visited.add(inputObject);

        boolean result;

        if (inputObject.isOneOf()) {
            // OneOf: unbreakable cycle if EVERY field has one (all escape routes blocked)
            result = true;
            for (GraphQLInputObjectField field : inputObject.getFieldDefinitions()) {
                if (!fieldTypeHasUnbreakableCycle(field.getType(), visited)) {
                    result = false;
                    break;
                }
            }
        } else {
            // Regular: unbreakable cycle if ANY non-null field has one
            result = false;
            for (GraphQLInputObjectField field : inputObject.getFieldDefinitions()) {
                if (isNonNull(field.getType())) {
                    GraphQLInputType nullableType = unwrapNonNullAs(field.getType());
                    if (fieldTypeHasUnbreakableCycle(nullableType, visited)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        visited.remove(inputObject);

        if (!result) {
            knownNoCycle.add(inputObject);
        }
        return result;
    }

    /**
     * Determines whether a field's type participates in an unbreakable cycle.
     *
     * @param fieldType the field type to check
     * @param visited   the set of input object types already visited in this traversal path
     * @return true if the field type has an unbreakable cycle
     */
    private boolean fieldTypeHasUnbreakableCycle(GraphQLInputType fieldType,
            Set<GraphQLInputObjectType> visited) {
        if (isList(fieldType)) {
            // Lists break cycles -- an empty list is always a valid value
            return false;
        }
        if (isNonNull(fieldType)) {
            return fieldTypeHasUnbreakableCycle(unwrapNonNullAs(fieldType), visited);
        }
        if (!(fieldType instanceof GraphQLInputObjectType)) {
            // Scalars, enums, etc. break cycles
            return false;
        }
        return hasUnbreakableCycle((GraphQLInputObjectType) fieldType, visited);
    }
}
