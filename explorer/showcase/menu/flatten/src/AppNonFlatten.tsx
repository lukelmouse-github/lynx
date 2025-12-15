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
              <view
              flatten={false}
        style={{
          marginTop: "20px",
          padding: "12px",
          backgroundColor: "#ffebee",
          borderRadius: "8px",
          width: "90%",
          borderLeft: "4px solid #f44336",
        }}
      >
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
              <view
              flatten={false}
        style={{
          marginTop: "20px",
          padding: "12px",
          backgroundColor: "#ffebee",
          borderRadius: "8px",
          width: "90%",
          borderLeft: "4px solid #f44336",
        }}
      >
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
              <view
              flatten={false}
        style={{
          marginTop: "20px",
          padding: "12px",
          backgroundColor: "#ffebee",
          borderRadius: "8px",
          width: "90%",
          borderLeft: "4px solid #f44336",
        }}
      ></view>
      </view>
      </view>
      </view>
      </view>
      </view>
      </view>
      </view>
      </view>
    </view>
  );
};
