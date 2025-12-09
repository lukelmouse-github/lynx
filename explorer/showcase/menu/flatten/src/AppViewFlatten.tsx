// Copyright 2024 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * 纯 View 的 Flatten Demo
 * 
 * 原理演示：
 * 这里的两个 <view> (蓝色和红色) 都没有设置 flatten={false}，也没有绑定事件。
 * 因此，Lynx 会将它们优化为 "FlattenUI"。
 * 
 * 在 Android 端：
 * 1. 不会创建 android.view.ViewGroup 对象。
 * 2. 它们只是父容器 Canvas 上的两次绘制操作 (drawRect)。
 * 3. 这种嵌套结构在渲染层被"拍平"了。
 */
export const AppViewFlatten = () => {
  return (
    <view
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "#f0f0f0",
      }}
    >
      {/* 父 View (Flatten): 蓝色背景 */}
      <view
        style={{
          width: "200px",
          height: "200px",
          backgroundColor: "#3498db",
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 View (Flatten): 红色背景 */}
        <view
          style={{
            width: "100px",
            height: "100px",
            backgroundColor: "#e74c3c",
          }}
        />
      </view>
    </view>
  );
};
