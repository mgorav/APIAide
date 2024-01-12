package com.gonnect.apiaide.oas;

import com.gonnect.apiaide.utils.CastUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.gonnect.apiaide.utils.CastUtil.castToList;
import static com.gonnect.apiaide.utils.CastUtil.castToMap;

public class AllOfMerger {

    public static Object mergeAllOfPropertiesHelper(Object obj) {
        if (obj instanceof Map<?, ?>) {
            Map<String, Object> objOut = new HashMap<>();
            Map<String, Object> objMap = castToMap(obj);
            for (Map.Entry<String, Object> entry : objMap.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if ("allOf".equals(k)) {
                    return mergeAllOfPropertiesHelper(merge(castToList(v)));
                } else if (v instanceof List<?>) {
                    objOut.put(k, mergeAllOfPropertiesHelperList(castToList(v)));
                } else if (v instanceof Map<?, ?>) {
                    objOut.put(k, mergeAllOfPropertiesHelper(v));
                } else {
                    objOut.put(k, v);
                }
            }
            return objOut;
        } else if (obj instanceof List<?>) {
            return mergeAllOfPropertiesHelperList(castToList(obj));
        } else {
            return obj;
        }
    }

    private static Object mergeAllOfPropertiesHelperList(List<Object> list) {
        return list.stream()
                .map(AllOfMerger::mergeAllOfPropertiesHelper)
                .collect(Collectors.toList());
    }

    private static Map<String, Object> merge(List<Object> toMerge) {
        List<Map<String, Object>> propertyList = new ArrayList<>();
        for (Object partialSchema : toMerge) {
            if (partialSchema instanceof Map<?, ?> && ((Map<?, ?>) partialSchema).containsKey("allOf")) {
                propertyList.addAll(castToList(((Map<?, ?>) partialSchema).get("allOf")));
            } else if (partialSchema instanceof Map<?, ?> && ((Map<?, ?>) partialSchema).containsKey("properties")) {
                propertyList.add(castToMap(((Map<?, ?>) partialSchema).get("properties")));
            } else {
                propertyList.add(castToMap(partialSchema));
            }
        }

        Map<String, Object> merged = new HashMap<>();
        merged.put("properties", propertyList.stream().map(m -> castToMap(m.get("properties"))).reduce(new HashMap<>(), AllOfMerger::mergeMap));
        List<Object> requiredList = propertyList.stream()
                .map(m -> m.get("required"))
                .filter(Objects::nonNull)
                .map(CastUtil::castToList)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        merged.put("required", requiredList);
        merged.put("type", "object");

        return merged;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> mergeMap(Map<K, V> base, Map<K, V> additional) {
        for (Map.Entry<K, V> entry : additional.entrySet()) {
            base.merge(entry.getKey(), entry.getValue(), (existingValue, newValue) -> {
                if (existingValue instanceof Map && newValue instanceof Map) {
                    return (V) mergeMap(castToMap(existingValue), castToMap(newValue));
                } else {
                    return newValue;
                }
            });
        }
        return base;
    }


    public static List<Map<String, Object>> mergeAllOfEndpoints(List<Map<String, Object>> endpoints) {
        return endpoints.stream()
                .map(endpoint -> Map.of(
                        "name", endpoint.get("name"),
                        "description", endpoint.get("description"),
                        "docs", mergeAllOfPropertiesHelper(endpoint.get("docs"))
                ))
                .collect(Collectors.toList());
    }
}
