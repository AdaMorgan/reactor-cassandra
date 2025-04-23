package com.github.compliance;

import com.github.adamorgan.annotations.UnknownNullability;
import com.github.adamorgan.api.managers.Manager;
import com.github.adamorgan.api.requests.ObjectAction;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jetbrains.annotations.Contract;
import org.junit.jupiter.api.Test;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

public class ArchUnitComplianceTest
{
    final JavaClasses apiClasses = new ClassFileImporter().importPackages("com.github.adamorgan.api");

    @Test
    void testMethodsThatReturnRestActionHaveCorrectAnnotations()
    {
        methods().that()
                .haveRawReturnType(assignableTo(ObjectAction.class))
                .and()
                .arePublic()
                .should()
                .beAnnotatedWith(CheckReturnValue.class)
                .andShould()
                .beAnnotatedWith(Nonnull.class)
                .check(apiClasses);
    }

    @Test
    void testMethodsThatReturnCompletableFutureHaveCorrectAnnotations()
    {
        methods().that()
                .haveRawReturnType(assignableTo(CompletableFuture.class))
                .and()
                .arePublic()
                .should()
                .beAnnotatedWith(CheckReturnValue.class)
                .andShould()
                .beAnnotatedWith(Nonnull.class)
                .check(apiClasses);
    }

    @Test
    void testMethodsThatReturnObjectShouldHaveNullabilityAnnotations()
    {
        methods().that()
                .haveRawReturnType(assignableTo(Object.class))
                .and()
                .arePublic()
                .and()
                .doNotHaveName("valueOf")
                .and()
                .doNotHaveName("toString")
                .should()
                .beAnnotatedWith(Nonnull.class)
                .orShould()
                .beAnnotatedWith(Nullable.class)
                .orShould()
                .beAnnotatedWith(Contract.class)
                .orShould()
                .beAnnotatedWith(UnknownNullability.class)
                .check(apiClasses);
    }

    @Test
    void testMethodsThatReturnPrimitivesShouldNotHaveNullabilityAnnotations()
    {
        methods().that()
                .haveRawReturnType(describe("primitive", JavaClass::isPrimitive))
                .and()
                .arePublic()
                .should()
                .notBeAnnotatedWith(Nonnull.class)
                .andShould()
                .notBeAnnotatedWith(Nullable.class)
                .check(apiClasses);
    }

    @Test
    void testRestActionClassesFollowNamePattern()
    {
        classes().that()
                .areAssignableTo(ObjectAction.class)
                .and()
                .areNotAssignableTo(Manager.class)
                .and()
                .arePublic()
                .should()
                .haveSimpleNameEndingWith("Action")
                .check(apiClasses);
    }

    @Test
    void testManagerClassesFollowNamePattern()
    {
        classes().that()
                .areAssignableTo(Manager.class)
                .and()
                .arePublic()
                .should()
                .haveSimpleNameEndingWith("Manager")
                .check(apiClasses);
    }

    @Test
    void testInternalClassesAreNotInApiPackage()
    {
        classes().that()
                .arePublic()
                .and()
                .haveSimpleNameEndingWith("Impl")
                .should()
                .resideOutsideOfPackage("com.github.adamorgan.api..")
                .allowEmptyShould(true)
                .check(apiClasses);
    }
}
