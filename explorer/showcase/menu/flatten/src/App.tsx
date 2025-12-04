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
 * ✅ 启用拍平的 Demo
 * 
 * 特点：
 * 1. 没有任何事件监听器 (bindtap)
 * 2. 样式简单，没有复杂的阴影或变换
 * 3. 结果：所有元素直接绘制在 Canvas 上，不创建 Android View
 */
export const App = () => {
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
          backgroundColor: "#f5f5f5",
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
          ✅ 启用拍平 (Flattened)
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
          此页面所有元素都被拍平。
          {"\n"}
          使用 Layout Inspector 查看时，你只能看到最外层的 LynxView，看不到内部的子元素（因为它们只是 Canvas 上的像素）。
        </text>

        {/* 拍平容器 */}
        <view
          style={{
            width: "90%",
            backgroundColor: "white",
            borderRadius: "10px",
            padding: "15px",
            border: "2px solid #4caf50",
          }}
        >
          {/* 元素1: 文本 */}
          <view style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}>
            <text style={{ fontSize: "12px", color: "#999", width: "30px" }}>①</text>
            <text style={{ fontSize: "16px", color: "#e74c3c", flex: 1 }}>
              文本节点
            </text>
          </view>

          {/* 元素2: 绿色圆形图片 */}
          <view style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}>
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
          <view style={{ display: "flex", flexDirection: "row", alignItems: "center", marginBottom: "12px" }}>
            <text style={{ fontSize: "12px", color: "#999", width: "30px" }}>③</text>
            <view
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
            backgroundColor: "#e8f5e8",
            borderRadius: "8px",
            width: "90%",
            borderLeft: "4px solid #4caf50",
          }}
        >
          <text
            style={{
              fontSize: "12px",
              color: "#2e7d32",
              lineHeight: "1.6",
            }}
          >
            ✅ 状态：已拍平
            {"\n"}
            所有 10 个元素都被合并绘制。
          </text>
        </view>
      </view>
    </scroll-view>
  );
};