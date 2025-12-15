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


      <text
        flatten={false}
        style={{
          backgroundColor: "#e0e0e0",
          fontSize: "18px",
          color: "black",
          fontWeight: "bold",
        }}
      >
        1-没拍平
      </text>

      {/* 父 View (Flatten): 蓝色背景 */}
      <view
        flatten={false}
        style={{
          width: "200px",
          backgroundColor: "#e74c3c", // Changed to red
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 Text (Flatten): 红色文本 */}
        <text
          flatten={false}
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
          2-1 没拍平
        </text>

        <text
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
         2-2 
        </text>

        <text
          flatten={false}
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
          2-3 没拍平
        </text>
      </view>

      <view
      flatten={false}
        style={{
          width: "200px",
          height: "50px",
          backgroundColor: "#3498db", // Changed to blue
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 Text (Flatten): 红色文本 */}
        <text
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
          3-1
        </text>

        <text
          flatten={false}
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
          3-2 没拍平
        </text>
        <text
          style={{
            width: "200px",
            height: "50px",
            fontSize: "18px",
            color: "black",
            fontWeight: "bold",
          }}
        >
          3-3
        </text>
      </view>
    </view>
  );
};
