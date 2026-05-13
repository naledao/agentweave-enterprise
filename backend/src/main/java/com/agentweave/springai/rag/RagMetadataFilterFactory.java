package com.agentweave.springai.rag;

import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class RagMetadataFilterFactory {

    public Optional<Filter.Expression> build(VectorRagSearchRequest request) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op expression = null;

        expression = and(builder, expression, eq(builder, "documentId", documentId(request)));
        expression = and(builder, expression, eq(builder, "businessDomain", request.normalizedBusinessDomain()));
        expression = and(builder, expression, eq(builder, "documentType", request.normalizedDocumentType()));
        expression = and(builder, expression, eq(builder, "permissionLevel", request.normalizedPermissionLevel()));

        return expression == null ? Optional.empty() : Optional.of(expression.build());
    }

    public Map<String, Object> describe(VectorRagSearchRequest request) {
        Map<String, Object> filter = new LinkedHashMap<>();
        putIfPresent(filter, "documentId", documentId(request));
        putIfPresent(filter, "businessDomain", request.normalizedBusinessDomain());
        putIfPresent(filter, "documentType", request.normalizedDocumentType());
        putIfPresent(filter, "permissionLevel", request.normalizedPermissionLevel());
        return filter;
    }

    private FilterExpressionBuilder.Op and(
            FilterExpressionBuilder builder,
            FilterExpressionBuilder.Op left,
            FilterExpressionBuilder.Op right) {
        if (right == null) {
            return left;
        }
        return left == null ? right : builder.and(left, right);
    }

    private FilterExpressionBuilder.Op eq(FilterExpressionBuilder builder, String key, String value) {
        if (value == null) {
            return null;
        }
        return builder.eq(key, value);
    }

    private String documentId(VectorRagSearchRequest request) {
        return request.documentId() == null ? null : request.documentId().toString();
    }

    private void putIfPresent(Map<String, Object> filter, String key, Object value) {
        if (value != null) {
            filter.put(key, value);
        }
    }
}
