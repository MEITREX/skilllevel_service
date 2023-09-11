package de.unistuttgart.iste.gits.skilllevel_service.exception;

import de.unistuttgart.iste.gits.common.exception.ExceptionToGraphQlErrorConverter;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Resolves exceptions thrown during data fetching.
 * Converts exceptions to {@link GraphQLError}s and logs them.
 */
@Component
@Slf4j
public class ExceptionResolver extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {
        return ExceptionToGraphQlErrorConverter.resolveToSingleError(ex, env);
    }
}