package benchmark;

import graphql.language.Document;
import graphql.schema.GraphQLSchema;

public class Scenario {
  public final GraphQLSchema schema;
  public final Document document;

  public Scenario(GraphQLSchema schema, Document document) {
    this.schema = schema;
    this.document = document;
  }
}
