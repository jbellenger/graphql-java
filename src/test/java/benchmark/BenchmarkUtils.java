package benchmark;

import com.google.common.io.Files;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGenerator;

import static graphql.Assert.assertTrue;

public class BenchmarkUtils {
    public static Scenario LARGE_SCHEMA_1 = loadScenario(
        "large-schema-1.graphqls",
        "large-schema-1-query.graphql"
    );

    public static Scenario LARGE_SCHEMA_4 = loadScenario(
        "large-schema-4.graphqls",
        "large-schema-4-query.graphql"
    );
    public static Scenario MANY_FRAGMENTS = loadScenario(
        "many-fragments.graphqls",
        "many-fragments-query.graphql"
    );

    public static Scenario loadScenario(String schemaPath, String queryPath) {
        String schemaString = loadResource(schemaPath);
        String query = loadResource(queryPath);
        GraphQLSchema schema = SchemaGenerator.createdMockedSchema(schemaString);
        Document document = Parser.parse(query);

        // make sure this is a valid query overall
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionResult executionResult = graphQL.execute(query);
        assertTrue(executionResult.getErrors().size() == 0);
        return new Scenario(schema, document);
    }

    static String loadResource(String name) {
        return asRTE(() -> {
            URL resource = BenchmarkUtils.class.getClassLoader().getResource(name);
            return String.join("\n", Files.readLines(new File(resource.toURI()), Charset.defaultCharset()));

        });
    }

    static <T> T asRTE(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
