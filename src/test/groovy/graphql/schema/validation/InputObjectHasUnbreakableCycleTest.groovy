package graphql.schema.validation

import graphql.TestUtil
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import spock.lang.Specification

/**
 * Tests for the unified InputObjectHasUnbreakableCycle validation rule,
 * which implements the spec's InputObjectHasUnbreakableCycle algorithm.
 *
 * Ported from graphql-js PR #4564 and the existing graphql-java tests.
 *
 * @see <a href="https://github.com/graphql/graphql-spec/pull/1211">graphql-spec PR #1211</a>
 * @see <a href="https://github.com/graphql/graphql-js/pull/4564">graphql-js PR #4564</a>
 */
class InputObjectHasUnbreakableCycleTest extends Specification {

    // ------------------------------------------------------------------
    // Valid schemas: breakable cycles and inhabited types
    // ------------------------------------------------------------------

    def "accepts an Input Object with breakable circular reference"() {
        def sdl = """
            type Query { f(arg: SomeInputObject): String }
            input SomeInputObject {
                self: SomeInputObject
                arrayOfSelf: [SomeInputObject]
                nonNullArrayOfSelf: [SomeInputObject]!
                nonNullArrayOfNonNullSelf: [SomeInputObject!]!
                intermediateSelf: AnotherInputObject
            }
            input AnotherInputObject {
                parent: SomeInputObject
            }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf Input Object with a scalar field"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { a: Int }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf Input Object with a recursive list field"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { a: [A] }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf Input Object referencing a non-OneOf input object"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B { x: Int }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf/OneOf cycle with a scalar escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B, escape: Int }
            input B @oneOf { a: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf/non-OneOf cycle with a nullable escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B { a: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a non-OneOf/non-OneOf cycle with a nullable escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A { b: B! }
            input B { a: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a non-OneOf/non-OneOf cycle with a list escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A { b: [B!]! }
            input B { a: A! }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf/non-OneOf with scalar escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B, escape: Int }
            input B { a: A! }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts multiple fields with chained oneOf escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B, c: C }
            input B @oneOf { a: A }
            input C @oneOf { a: A, escape: String }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf with enum field"() {
        def sdl = """
            type Query { f(arg: A): String }
            enum Color { RED GREEN BLUE }
            input A @oneOf { a: Color }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts oneOf with scalar fields"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { a: String, b: Int }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts mutually referencing oneOf types with scalar escape"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B @oneOf { a: A, escape: Int }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts oneOf referencing non-oneOf with back-reference"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: RegularInput }
            input RegularInput { back: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    def "accepts a OneOf Input Object with a non-null recursive list field"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { a: [A!] }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        noExceptionThrown()
    }

    // ------------------------------------------------------------------
    // Invalid schemas: unbreakable cycles
    // ------------------------------------------------------------------

    def "rejects an Input Object with non-breakable self-reference"() {
        def sdl = """
            type Query { f(arg: SomeInputObject): String }
            input SomeInputObject {
                nonNullSelf: SomeInputObject!
            }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.any {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
    }

    def "rejects Input Objects with non-breakable circular reference spread across them"() {
        def sdl = """
            type Query { f(arg: SomeInputObject): String }
            input SomeInputObject {
                startLoop: AnotherInputObject!
            }
            input AnotherInputObject {
                nextInLoop: YetAnotherInputObject!
            }
            input YetAnotherInputObject {
                closeLoop: SomeInputObject!
            }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.every {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
        // Each type in the cycle gets its own error
        schemaProblem.errors.size() == 3
    }

    def "rejects Input Objects with multiple non-breakable circular references"() {
        def sdl = """
            type Query { f(arg: SomeInputObject): String }
            input SomeInputObject {
                startLoop: AnotherInputObject!
            }
            input AnotherInputObject {
                closeLoop: SomeInputObject!
                startSecondLoop: YetAnotherInputObject!
            }
            input YetAnotherInputObject {
                closeSecondLoop: AnotherInputObject!
                nonNullSelf: YetAnotherInputObject!
            }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.every {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
        // All three types participate in unbreakable cycles
        schemaProblem.errors.size() == 3
    }

    def "rejects a self-referencing OneOf type with no escapes"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { self: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.size() == 1
        schemaProblem.errors[0].classification == SchemaValidationErrorType.UnbrokenInputCycle
    }

    def "rejects a mixed OneOf/non-OneOf cycle with no escapes"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B { a: A! }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.every {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
        // Both A and B have unbreakable cycles
        schemaProblem.errors.size() == 2
    }

    def "rejects a larger mixed OneOf/non-OneOf cycle with no escapes"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B { c: C! }
            input C @oneOf { a: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.every {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
        // All three types participate in the cycle
        schemaProblem.errors.size() == 3
    }

    def "rejects multiple oneOf types forming a cycle with no escapes"() {
        def sdl = """
            type Query { f(arg: A): String }
            input A @oneOf { b: B }
            input B @oneOf { c: C }
            input C @oneOf { a: A }
        """

        when:
        def registry = new SchemaParser().parse(sdl)
        new SchemaGenerator().makeExecutableSchema(registry, TestUtil.getMockRuntimeWiring())

        then:
        def schemaProblem = thrown(InvalidSchemaException)
        schemaProblem.errors.every {
            it.classification == SchemaValidationErrorType.UnbrokenInputCycle
        }
        schemaProblem.errors.size() == 3
    }
}
