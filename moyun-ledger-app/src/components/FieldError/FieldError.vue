<template>
  <text v-if="msg" class="field-error">{{ msg }}</text>
</template>

<script>
export default {
  name: 'FieldError',
  props: { msg: { type: String, default: '' } }
};
</script>

<style scoped>
.field-error {
  display: block;
  padding: 0 0 0 170rpx;
  margin: -8rpx 0 8rpx;
  font-size: 22rpx;
  color: var(--expense);
}
</style>
