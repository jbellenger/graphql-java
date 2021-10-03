package graphql;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.dataloader.DataLoaderRegistry;

import graphql.cachecontrol.CacheControl;
import graphql.execution.ExecutionId;

import static graphql.Assert.assertNotNull;

public interface ExecutionInput {
  String getQuery();

  String getOperationName();

  @Deprecated
  Object getContext();

  GraphQLContext getGraphQLContext();

  Object getLocalContext();

  Object getRoot();

  Map<String, Object> getVariables();

  DataLoaderRegistry getDataLoaderRegistry();

  CacheControl getCacheControl();

  ExecutionId getExecutionId();

  Locale getLocale();

  Map<String, Object> getExtensions();

  ExecutionInput transform(Consumer<Builder> builderConsumer);

  static Builder newExecutionInput() {
    return ExecutionInputImpl.newExecutionInput();
  }

  /**
   * Creates a new builder of ExecutionInput objects with the given query
   *
   * @param query the query to execute
   *
   * @return a new builder of ExecutionInput objects
   */
  static Builder newExecutionInput(String query) {
    return newExecutionInput().query(query);
  }

  interface Builder {
    Builder query(String query);

    Builder operationName(String operationName);

    /**
     * A default one will be assigned, but you can set your own.
     *
     * @param executionId an execution id object
     * @return this builder
     */
    Builder executionId(ExecutionId executionId);

    /**
     * Sets the locale to use for this operation
     *
     * @param locale the locale to use
     * @return this builder
     */
    Builder locale(Locale locale);

    /**
     * Sets initial localContext in root data fetchers
     *
     * @param localContext the local context to use
     * @return this builder
     */
    Builder localContext(Object localContext);


    /**
     * The legacy context object
     *
     * @param context the context object to use
     * @return this builder
     * @deprecated - the {@link ExecutionInput#getGraphQLContext()} is a fixed mutable instance now
     */
    @Deprecated
    Builder context(Object context);

    /**
     * The legacy context object
     *
     * @param contextBuilder the context builder object to use
     * @return this builder
     * @deprecated - the {@link ExecutionInput#getGraphQLContext()} is a fixed mutable instance now
     */
    @Deprecated
    Builder context(GraphQLContext.Builder contextBuilder);

    /**
     * The legacy context object
     *
     * @param contextBuilderFunction the context builder function to use
     * @return this builder
     * @deprecated - the {@link ExecutionInput#getGraphQLContext()} is a fixed mutable instance now
     */
    @Deprecated
    Builder context(UnaryOperator<GraphQLContext.Builder> contextBuilderFunction);

    /**
     * This will give you a builder of {@link GraphQLContext} and any values you set will be copied
     * into the underlying {@link GraphQLContext} of this execution input
     *
     * @param builderFunction a builder function you can use to put values into the context
     * @return this builder
     */
    Builder graphQLContext(Consumer<GraphQLContext.Builder> builderFunction);

    /**
     * This will put all the values from the map into the underlying {@link GraphQLContext} of this execution input
     *
     * @param mapOfContext a map of values to put in the context
     * @return this builder
     */
    Builder graphQLContext(Map<?, Object> mapOfContext);

    // JMB TODO: add javadocs for some of these un-doc'd methods
    Builder root(Object root);

    Builder variables(Map<String, Object> variables);

    Builder extensions(Map<String, Object> extensions);

    /**
     * You should create new {@link org.dataloader.DataLoaderRegistry}s and new {@link org.dataloader.DataLoader}s for each execution.  Do not
     * re-use
     * instances as this will create unexpected results.
     *
     * @param dataLoaderRegistry a registry of {@link org.dataloader.DataLoader}s
     * @return this builder
     */
    Builder dataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry);

    Builder cacheControl(CacheControl cacheControl);

    ExecutionInput build();
  }
}
