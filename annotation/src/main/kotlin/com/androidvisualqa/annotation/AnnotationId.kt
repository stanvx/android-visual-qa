package com.androidvisualqa.annotation

/**
 * Local annotation identifier within a single draft.
 *
 * This is distinct from the model-layer [com.androidvisualqa.model.annotation] identifier;
 * it is scoped to a single editor session and serialised into the report during save.
 */
@JvmInline
public value class AnnotationId(public val value: String)
