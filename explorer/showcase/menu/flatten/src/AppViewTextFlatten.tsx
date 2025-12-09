// Copyright 2024 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * View + Text 的 Flatten Demo
 *
 * 原理演示：
 * 这里的 <view> 和 <text> 都没有设置 flatten={false}，也没有绑定事件。
 * 因此，Lynx 会将它们优化为 "FlattenUI" 和 "FlattenUIText"。
 *
 * 在 Android 端：
 * 1. <view> 不会创建 android.view.ViewGroup 对象。
 * 2. <text> 不会创建 android.widget.TextView 对象。
 * 3. 它们只是父容器 Canvas 上的两次绘制操作：
 *    - 一次 drawRect 绘制蓝色背景。
 *    - 一次 drawText 绘制红色文本。
 * 4. 这种结合是 Flatten 最常见的使用场景。
 */
export const AppViewTextFlatten = () => {
  return (
    <view
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column", // Changed from default row to column
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "#f0f0f0",
      }}
    >
      {/* 父 View (Flatten): 蓝色背景 */}
      <view
        style={{
          width: "200px",
          height: "100px",
          backgroundColor: "#3498db",
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 Text (Flatten): 红色文本 */}
        <text
          style={{
            fontSize: "18px",
            color: "#e74c3c",
            fontWeight: "bold",
          }}
        >
          1-Hello Flatten!
        </text>
      </view>

      <view
        style={{
          width: "200px",
          height: "100px",
          backgroundColor: "#3498db",
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 Text (Flatten): 红色文本 */}
        <text
          style={{
            fontSize: "18px",
            color: "#8e44ad", // Changed to purple
            fontWeight: "bold",
          }}
        >
          2-Hello View!
        </text>

                <text
          style={{
            fontSize: "18px",
            color: "#2980b9", // Changed to blue
            fontWeight: "bold",
          }}
        >
          2-1-Hello View!
        </text>
      </view>
    </view>
  );
};
