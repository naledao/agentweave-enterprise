<template>
  <section class="forbidden-view">
    <el-result
      icon="warning"
      title="无权限访问"
      sub-title="当前账号没有访问该页面的权限。"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/app/chat')">返回对话</el-button>
      </template>
    </el-result>
  </section>
</template>

<style scoped>
.forbidden-view {
  display: grid;
  min-height: 460px;
  place-items: center;
}
</style>
