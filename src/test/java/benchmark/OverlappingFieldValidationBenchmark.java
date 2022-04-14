package benchmark;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import graphql.validation.LanguageTraversal;
import graphql.validation.RulesVisitor;
import graphql.validation.ValidationContext;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorCollector;
import graphql.validation.rules.OverlappingFieldsCanBeMerged;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Threads(1)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 10)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OverlappingFieldValidationBenchmark {

    @Benchmark
    public void manyFragments(Blackhole blackhole) {
        blackhole.consume(validateQuery(BenchmarkUtils.MANY_FRAGMENTS));
    }

    private List<ValidationError> validateQuery(Scenario scenario) {
        ValidationErrorCollector errorCollector = new ValidationErrorCollector();
        ValidationContext validationContext = new ValidationContext(scenario.schema, scenario.document);
        OverlappingFieldsCanBeMerged overlappingFieldsCanBeMerged = new OverlappingFieldsCanBeMerged(validationContext, errorCollector);
        LanguageTraversal languageTraversal = new LanguageTraversal();
        languageTraversal.traverse(scenario.document, new RulesVisitor(validationContext, Collections.singletonList(overlappingFieldsCanBeMerged)));
        return errorCollector.getErrors();
    }
}
