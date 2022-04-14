package benchmark;

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

import graphql.validation.Validator;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Threads(1)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 10)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ValidatorBenchmark {
    private void run(Scenario scenario) {
        Validator validator = new Validator();
        validator.validateDocument(scenario.schema, scenario.document);
    }

    /*
    @Benchmark
    public void largeSchema1(MyState state) {
        run(state.largeSchema1);
    }

    @Benchmark
    public void largeSchema4(MyState state) {
        run(state.largeSchema4);
    }
     */

    @Benchmark
    public void manyFragments() {
        run(BenchmarkUtils.MANY_FRAGMENTS);
    }
}
