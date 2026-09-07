<template>
  <view></view>
</template>

<script>
export default {
  onLoad() {
    // 日历账单已合并到流水明细页（同页 tab 切换），旧路由重定向过去
    uni.redirectTo({ url: '/pages/record/list' });
  }
};
</script>
