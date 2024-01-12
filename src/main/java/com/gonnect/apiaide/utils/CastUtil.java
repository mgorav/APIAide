package com.gonnect.apiaide.utils;

import java.util.Map;
import java.util.List;

public class CastUtil {

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> castToMap(Object obj) {
        if (obj instanceof Map<?, ?>) {
            return (Map<K, V>) obj;
        } else {
            throw new ClassCastException("Cannot cast object to Map");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> castToList(Object obj) {
        if (obj instanceof List<?>) {
            return (List<T>) obj;
        } else {
            throw new ClassCastException("Cannot cast object to List");
        }
    }
}
