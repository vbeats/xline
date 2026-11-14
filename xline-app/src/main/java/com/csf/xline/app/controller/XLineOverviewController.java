package com.csf.xline.app.controller;

import com.csf.xline.core.INode;
import com.csf.xline.core.Node;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.enums.NodeType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class XLineOverviewController {

    private final XLineProperties properties;

    public XLineOverviewController(XLineProperties properties) {
        this.properties = properties;
    }

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/api/xline/overview")
    @ResponseBody
    public Map<String, Object> overview() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Node> configuredNodes = properties.getNodes();
        if (configuredNodes != null) {
            for (Node configuredNode : configuredNodes) {
                nodes.add(nodeView(configuredNode));
            }
        }

        List<Map<String, String>> edges = new ArrayList<>();
        Map<String, List<String>> struct = properties.getStruct();
        if (struct != null) {
            for (Map.Entry<String, List<String>> entry : struct.entrySet()) {
                List<String> targets = entry.getValue();
                if (targets == null) {
                    continue;
                }
                for (String target : targets) {
                    if (target != null && !target.trim().isEmpty()) {
                        Map<String, String> edge = new LinkedHashMap<>();
                        edge.put("source", entry.getKey());
                        edge.put("target", target);
                        edges.add(edge);
                    }
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nodes", nodes);
        response.put("edges", edges);
        return response;
    }

    private Map<String, Object> nodeView(Node node) {
        Map<String, Object> view = new LinkedHashMap<>();
        String name = node.getName();
        INode registered = name == null ? null : com.csf.xline.core.XLineContext.NODES.get(name);
        NodeType type = node.getType();
        view.put("name", name);
        view.put("type", type == null ? null : type.getSegment());
        view.put("status", registered == null ? "PENDING" : "READY");
        view.put("properties", propertiesView(node));
        return view;
    }

    private Map<String, Object> propertiesView(Node node) {
        Map<String, Object> propertiesView = new LinkedHashMap<>();
        addProperties(propertiesView, "mysql", node.getMysql());
        addProperties(propertiesView, "postgresql", node.getPostgresql());
        addProperties(propertiesView, "sqlite", node.getSqlite());
        addProperties(propertiesView, "mssql", node.getMssql());
        addProperties(propertiesView, "tcp", node.getTcp());
        addProperties(propertiesView, "http", node.getHttp());
        addProperties(propertiesView, "serialPort", node.getSerialPort());
        if (node.getExtra() != null && !node.getExtra().isEmpty()) {
            propertiesView.put("extra", serialize(node.getExtra()));
        }
        return propertiesView;
    }

    private void addProperties(Map<String, Object> target, String name, Object value) {
        if (value != null) {
            target.put(name, serialize(value));
        }
    }

    private Object serialize(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum) {
            return value;
        }
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                result.put(String.valueOf(entry.getKey()), serialize(entry.getValue()));
            }
            return result;
        }
        if (value instanceof Iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                result.add(serialize(item));
            }
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Field field : fields(value.getClass())) {
            try {
                field.setAccessible(true);
                result.put(field.getName(), serialize(field.get(value)));
            } catch (IllegalAccessException ignored) {
                // Public configuration objects are reflectively readable.
            }
        }
        return result;
    }

    private List<Field> fields(Class<?> type) {
        if (type == null || type == Object.class) {
            return Collections.emptyList();
        }
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fields.add(field);
            }
        }
        fields.addAll(fields(type.getSuperclass()));
        return fields;
    }

}
