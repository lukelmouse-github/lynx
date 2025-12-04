// Copyright 2024 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { ReactElement } from "react";

// Sample images for demonstration (using SVG placeholders)
const SAMPLE_IMAGE_GREEN = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40'%3E%3Ccircle cx='20' cy='20' r='18' fill='%2300ff00'/%3E%3C/svg%3E";
const SAMPLE_IMAGE_BLUE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40'%3E%3Crect width='40' height='40' fill='%230066ff' rx='5'/%3E%3C/svg%3E";
const SAMPLE_IMAGE_RED = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40'%3E%3Cpolygon points='20,5 35,35 5,35' fill='%23ff0000'/%3E%3C/svg%3E";
const SAMPLE_IMAGE_YELLOW = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40'%3E%3Crect width='40' height='40' fill='%23ffcc00' rx='20'/%3E%3C/svg%3E";
const SAMPLE_IMAGE_PURPLE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40'%3E%3Crect width='40' height='40' fill='%239933ff' rx='8'/%3E%3C/svg%3E";

/**
 * ❌ 禁用拍平的 Demo
 * 
 * 特点：
 * 1. 使用 flatten={false} 属性显式禁用拍平
 * 2. 这会强制 Lynx 为每个元素创建独立的 Android View
 * 3. 结果：Layout Inspector 可以看到完整的 View 树
 */
export const AppNonFlatten = () => {
  return (
    <scroll-view
      scroll-orientation="vertical"
      style={{
        width: "100%",
        height: "100%",
        backgroundColor: "#fff",
      }}
    >
      <view
        style={{
          width: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          padding: "20px",
          backgroundColor: "#fafafa",
        }}
      >
        <text
          style={{
            fontSize: "24px",
            fontWeight: "bold",
            marginBottom: "10px",
            color: "#333",
          }}
        >
          ❌ 禁用拍平 (Non-Flattened)
        </text>

        <text
          style={{
            fontSize: "14px",
            marginBottom: "20px",
            textAlign: "center",
            color: "#666",
            width: "90%",
          }}
        >
          此页面所有元素都**未**被拍平。
          {"\n"}
          使用 Layout Inspector 查看时，你可以看到完整的 View 树结构，每个元素对应一个 Android View。
        </text>

        {/* 非拍平容器 - 使用 flatten={false} 禁用拍平 */}
        <view
          // @ts-ignore
          flatten={false}
          style={{
            width: "90%",
            backgroundColor: "white",
            borderRadius: "10px",
            padding: "15px",
            border: "2px solid #f44336",
          }}
        >
          {/* 元素1: 文本 */}
          <view 
            // @ts-ignore
            flatten={false}
            style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}
          >
            <text style={{ fontSize: "12px", color: "#999", width: "30px" }}>①</text>
            <text style={{ fontSize: "16px", color: "#e74c3c", flex: 1 }}>
              文本节点
            </text>
          </view>

          {/* 元素2: 绿色圆形图片 */}
          <view 
            // @ts-ignore
            flatten={false}
            style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}
          >
            <text style={{ fontSize: "12px", color: "#999", width: "30px" }}>②</text>
            <image
              src={SAMPLE_IMAGE_GREEN}
              style={{
                width: "40px",
                height: "40px",
                marginRight: "10px",
              }}
            />
            <text style={{ fontSize: "14px", color: "#27ae60" }}>绿色圆形</text>
          </view>

          {/* 元素3: 蓝色矩形View */}
          <view 
            // @ts-ignore
            flatten={false}
            style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}
          >
            <text style={{ fontSize: "12px", color: "#999", width: "30px" }}>③</text>
            <view
              // @ts-ignore
              flatten={false}
              style={{
                width: "60px",
                height: "30px",
                backgroundColor: "#3498db",
                borderRadius: "5px",
                marginRight: "10px",
              }}
            />
            <text style={{ fontSize: "14px", color: "#3498db" }}>蓝色矩形</text>
          </view>
        </view>

        {/* 说明信息 */}
        <view
          style={{
            marginTop: "20px",
            padding: "12px",
            backgroundColor: "#ffebee",
            borderRadius: "8px",
            width: "90%",
            borderLeft: "4px solid #f44336",
          }}
        >
          <text
            style={{
              fontSize: "12px",
              color: "#d32f2f",
              lineHeight: "1.6",
            }}
          >
            ❌ 状态：未拍平
            {"\n"}
            所有 10 个元素都创建了独立的 Android View 对象。
          </text>
        </view>
      </view>
    </scroll-view>
  );
};
