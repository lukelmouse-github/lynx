// Copyright 2019 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.lynx.tasm.base;

import androidx.annotation.Nullable;

/**
 * 绘制链表操作记录数据结构
 * 用于记录插入(insert)和删除(remove)操作的关键信息，方便序列化输出到日志
 */
public class DrawListOperation {
    // 操作类型: "insert" 或 "remove"
    private final String operation;
    // 父节点标签名，如 "page", "view"
    private final String parentTagName;
    // 父节点签名(唯一标识)
    private final int parentSign;
    // 插入位置索引（对于插入操作有效，删除操作可为-1）
    private final int insertIndex;
    // 当前节点标签名
    private final String nodeTagName;
    // 当前节点签名(唯一标识)
    private final int nodeSign;
    // 当前节点类型（类名）
    private final String nodeType;
    // 时间戳（可选，便于排序）
    private final long timestamp;

    public DrawListOperation(String operation, String parentTagName, int parentSign,
                             int insertIndex, String nodeTagName, int nodeSign, String nodeType) {
        this.operation = operation;
        this.parentTagName = parentTagName;
        this.parentSign = parentSign;
        this.insertIndex = insertIndex;
        this.nodeTagName = nodeTagName;
        this.nodeSign = nodeSign;
        this.nodeType = nodeType;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构建插入操作记录
     */
    public static DrawListOperation forInsert(String parentTagName, int parentSign,
                                              int insertIndex, String nodeTagName, int nodeSign, String nodeType) {
        return new DrawListOperation("insert", parentTagName, parentSign,
                insertIndex, nodeTagName, nodeSign, nodeType);
    }

    /**
     * 构建删除操作记录
     */
    public static DrawListOperation forRemove(String parentTagName, int parentSign,
                                              String nodeTagName, int nodeSign, String nodeType) {
        return new DrawListOperation("remove", parentTagName, parentSign,
                -1, nodeTagName, nodeSign, nodeType);
    }

    /**
     * 将对象序列化为JSON字符串
     * 格式示例：
     * {
     *   "operation": "insert",
     *   "parent": "page[10]",
     *   "parentTagName": "page",
     *   "parentSign": 10,
     *   "insertIndex": 0,
     *   "node": "view[13]",
     *   "nodeTagName": "view",
     *   "nodeSign": 13,
     *   "nodeType": "LynxFlattenUI",
     *   "timestamp": 1630000000000
     * }
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendField(sb, "operation", operation, false);
        sb.append(",");
        // 为了方便阅读，同时提供组合字符串和分拆字段
        appendField(sb, "parent", parentTagName + "[" + parentSign + "]", false);
        sb.append(",");
        appendField(sb, "parentTagName", parentTagName, false);
        sb.append(",");
        sb.append("\"parentSign\":").append(parentSign);
        sb.append(",");
        sb.append("\"insertIndex\":").append(insertIndex);
        sb.append(",");
        appendField(sb, "node", nodeTagName + "[" + nodeSign + "]", false);
        sb.append(",");
        appendField(sb, "nodeTagName", nodeTagName, false);
        sb.append(",");
        sb.append("\"nodeSign\":").append(nodeSign);
        sb.append(",");
        appendField(sb, "nodeType", nodeType, false);
        sb.append(",");
        sb.append("\"timestamp\":").append(timestamp);
        sb.append("}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value, boolean isLast) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\"");
        }
    }

    /**
     * 简单JSON字符串转义：将 " 转义为 \"，\ 转义为 \\
     */
    private String escapeJson(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }

    // Getter方法（可选）
    public String getOperation() { return operation; }
    public String getParentTagName() { return parentTagName; }
    public int getParentSign() { return parentSign; }
    public int getInsertIndex() { return insertIndex; }
    public String getNodeTagName() { return nodeTagName; }
    public int getNodeSign() { return nodeSign; }
    public String getNodeType() { return nodeType; }
    public long getTimestamp() { return timestamp; }
}

