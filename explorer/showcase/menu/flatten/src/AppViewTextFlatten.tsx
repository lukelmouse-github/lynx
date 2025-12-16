// Copyright 2024 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { useState } from "@lynx-js/react";
import type { TouchEvent } from "@lynx-js/types";

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
  // 管理第一个 View 的颜色状态
  const [view1Color, setView1Color] = useState<string>("#e74c3c");
  // 管理第二个 View 的颜色状态
  const [view2Color, setView2Color] = useState<string>("#3498db");

  // 管理第二个 View 内三个子 text 的颜色状态
  const [text31Color, setText31Color] = useState<string>("black");
  const [text32Color, setText32Color] = useState<string>("black");
  const [text33Color, setText33Color] = useState<string>("black");

  function handleTap(e: TouchEvent) {
    console.log("被点击的元素 ID:", e.currentTarget.id);

    // 根据点击的元素 ID 改变对应 View 的颜色
    if (e.currentTarget.id === "view1") {
      // 在红色和绿色之间切换
      setView1Color(view1Color === "#e74c3c" ? "#27ae60" : "#e74c3c");
    } else if (e.currentTarget.id === "view2") {
      // 在蓝色和橙色之间切换
      setView2Color(view2Color === "#3498db" ? "#f39c12" : "#3498db");
    } else if (e.currentTarget.id === "text31") {
      // 在黑色和红色之间切换
      setText31Color(text31Color === "black" ? "#e74c3c" : "black");
    } else if (e.currentTarget.id === "text32") {
      // 在黑色和绿色之间切换
      setText32Color(text32Color === "black" ? "#27ae60" : "black");
    } else if (e.currentTarget.id === "text33") {
      // 在黑色和蓝色之间切换
      setText33Color(text33Color === "black" ? "#3498db" : "black");
    }
  }
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

      {/* 父 View (Flatten): 可点击改变颜色 */}
      <view
        id="view1"
        flatten={false}
        bindtap={handleTap}
        style={{
          width: "200px",
          backgroundColor: view1Color,
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
        id="view2"
        flatten={false}
        bindtap={handleTap}
        style={{
          width: "200px",
          height: "50px",
          backgroundColor: view2Color,
          justifyContent: "center",
          alignItems: "center",
          display: "flex",
        }}
      >
        {/* 子 Text (Flatten): 红色文本 */}
        <text

          id="text31"

          bindtap={handleTap}

          style={{

            width: "200px",

            height: "50px",

            fontSize: "18px",

            color: text31Color,

            fontWeight: "bold",

          }}

        >

          3-1-可以点击

        </text>




        <text

          id="text32"

          flatten={false}

          bindtap={handleTap}

          style={{

            width: "200px",

            height: "50px",

            fontSize: "18px",

            color: text32Color,

            fontWeight: "bold",

          }}

        >

          3-2 没拍平-可以点击

        </text>

        <text
          id="text33"
          bindtap={handleTap}

          style={{

            width: "200px",

            height: "50px",

            fontSize: "18px",

            color: text33Color,

            fontWeight: "bold",

          }}

        >

          3-3-可以点击

        </text>
      </view>
    </view>
  );
};
