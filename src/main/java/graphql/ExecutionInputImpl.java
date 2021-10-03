package graphql;

import graphql.cachecontrol.CacheControl;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationState;
import org.dataloader.DataLoaderRegistry;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static graphql.Assert.assertNotNull;

/**
 * This represents the series of values that can be input on a graphql query execution
 */
@Internal
public class ExecutionInputImpl implements ExecutionInput {
    private final String query;
    private final String operationName;
    private final Object context;
    private final GraphQLContext graphQLContext;
    private final Object localContext;
    private final Object root;
    private final Map<String, Object> variables;
    private final Map<String, Object> extensions;
    private final DataLoaderRegistry dataLoaderRegistry;
    private final CacheControl cacheControl;
    private final ExecutionId executionId;
    private final Locale locale;


    @Internal
    private ExecutionInputImpl(BuilderImpl builder) {
        this.query = assertNotNull(builder.query, () -> "query can't be null");
        this.operationName = builder.operationName;
        this.context = builder.context;
        this.graphQLContext = assertNotNull(builder.graphQLContext);
        this.root = builder.root;
        this.variables = builder.variables;
        this.dataLoaderRegistry = builder.dataLoaderRegistry;
        this.cacheControl = builder.cacheControl;
        this.executionId = builder.executionId;
        this.locale = builder.locale;
        this.localContext = builder.localContext;
        this.extensions = builder.extensions;
    }

    /**
     * @return the query text
     */
    @Override
    public String getQuery() {
        return query;
    }

    /**
     * @return the name of the query operation
     */
    @Override
    public String getOperationName() {
        return operationName;
    }

    /**
     * The legacy context object has been deprecated in favour of the more shareable
     * {@link #getGraphQLContext()}
     *
     * @return the context object to pass to all data fetchers
     *
     * @deprecated - use {@link #getGraphQLContext()}
     */
    @Override
    @Deprecated
    public Object getContext() {
        return context;
    }

    /**
     * @return the shared {@link GraphQLContext} object to pass to all data fetchers
     */
    @Override
    public GraphQLContext getGraphQLContext() {
        return graphQLContext;
    }

    /**
     * @return the local context object to pass to all top level (i.e. query, mutation, subscription) data fetchers
     */
    @Override
    public Object getLocalContext() {
        return localContext;
    }

    /**
     * @return the root object to start the query execution on
     */
    @Override
    public Object getRoot() {
        return root;
    }

    /**
     * @return a map of variables that can be referenced via $syntax in the query
     */
    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * @return the data loader registry associated with this execution
     */
    @Override
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    /**
     * @return the cache control helper associated with this execution
     */
    @Override
    public CacheControl getCacheControl() {
        return cacheControl;
    }

    /**
     * @return Id that will be/was used to execute this operation.
     */
    @Override
    public ExecutionId getExecutionId() {
        return executionId;
    }

    /**
     * This returns the locale of this operation.
     *
     * @return the locale of this operation
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * @return a map of extension values that can be sent in to a request
     */
    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * This helps you transform the current ExecutionInput object into another one by starting a builder with all
     * the current values and allows you to transform it how you want.
     *
     * @param builderConsumer the consumer code that will be given a builder to transform
     *
     * @return a new ExecutionInput object based on calling build on that builder
     */
    @Override
    public ExecutionInput transform(Consumer<Builder> builderConsumer) {
        Builder builder = new BuilderImpl()
                .query(this.query)
                .operationName(this.operationName)
                .context(this.context)
                .localContext(this.localContext)
                .transfer(this.graphQLContext)
                .root(this.root)
                .dataLoaderRegistry(this.dataLoaderRegistry)
                .cacheControl(this.cacheControl)
                .variables(this.variables)
                .extensions(this.extensions)
                .executionId(this.executionId)
                .locale(this.locale);

        builderConsumer.accept(builder);

        return builder.build();
    }

    @Override
    public String toString() {
        return "ExecutionInput{" +
                "query='" + query + '\'' +
                ", operationName='" + operationName + '\'' +
                ", context=" + context +
                ", graphQLContext=" + graphQLContext +
                ", root=" + root +
                ", variables=" + variables +
                ", dataLoaderRegistry=" + dataLoaderRegistry +
                ", executionId= " + executionId +
                ", locale= " + locale +
                '}';
    }

    public static Builder newExecutionInput() {
        return new BuilderImpl();
    }

    private static class BuilderImpl implements ExecutionInput.Builder {

        private String query;
        private String operationName;
        private GraphQLContext graphQLContext = GraphQLContext.newContext().build();
        private Object context = graphQLContext; // we make these the same object on purpose - legacy code will get the same object if this change nothing
        private Object localContext;
        private Object root;
        private Map<String, Object> variables = Collections.emptyMap();
        public Map<String, Object> extensions = Collections.emptyMap();
        //
        // this is important - it allows code to later known if we never really set a dataloader and hence it can optimize
        // dataloader field tracking away.
        //
        private DataLoaderRegistry dataLoaderRegistry = DataLoaderDispatcherInstrumentationState.EMPTY_DATALOADER_REGISTRY;
        private CacheControl cacheControl = CacheControl.newCacheControl();
        private Locale locale;
        private ExecutionId executionId;

        @Override
        public BuilderImpl query(String query) {
            this.query = assertNotNull(query, () -> "query can't be null");
            return this;
        }

        @Override
        public BuilderImpl operationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        @Override
        public BuilderImpl executionId(ExecutionId executionId) {
            this.executionId = executionId;
            return this;
        }

        @Override
        public BuilderImpl locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        @Override
        public BuilderImpl localContext(Object localContext) {
            this.localContext = localContext;
            return this;
        }

        @Override
        public BuilderImpl context(Object context) {
            this.context = context;
            return this;
        }

        @Override
        public BuilderImpl context(GraphQLContext.Builder contextBuilder) {
            this.context = contextBuilder.build();
            return this;
        }

        @Override
        public BuilderImpl context(UnaryOperator<GraphQLContext.Builder> contextBuilderFunction) {
            GraphQLContext.Builder builder = GraphQLContext.newContext();
            builder = contextBuilderFunction.apply(builder);
            return context(builder.build());
        }

        public BuilderImpl graphQLContext(Consumer<GraphQLContext.Builder> builderFunction) {
            GraphQLContext.Builder builder = GraphQLContext.newContext();
            builderFunction.accept(builder);
            this.graphQLContext.putAll(builder);
            return this;
        }

        @Override
        public BuilderImpl graphQLContext(Map<?, Object> mapOfContext) {
            this.graphQLContext.putAll(mapOfContext);
            return this;
        }

        // JMB TODO: update comment?
        // hidden on purpose
        private BuilderImpl transfer(GraphQLContext graphQLContext) {
            this.graphQLContext = Assert.assertNotNull(graphQLContext);
            return this;
        }

        @Override
        public BuilderImpl root(Object root) {
            this.root = root;
            return this;
        }

        @Override
        public BuilderImpl variables(Map<String, Object> variables) {
            this.variables = assertNotNull(variables, () -> "variables map can't be null");
            return this;
        }

        @Override
        public BuilderImpl extensions(Map<String, Object> extensions) {
            this.extensions = assertNotNull(extensions, () -> "extensions map can't be null");
            return this;
        }

        @Override
        public BuilderImpl dataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry) {
            this.dataLoaderRegistry = assertNotNull(dataLoaderRegistry);
            return this;
        }

        @Override
        public BuilderImpl cacheControl(CacheControl cacheControl) {
            this.cacheControl = assertNotNull(cacheControl);
            return this;
        }

        @Override
        public ExecutionInputImpl build() {
            return new ExecutionInputImpl(this);
        }
    }
}
