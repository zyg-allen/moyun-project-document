<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库" />

    <!-- 仓库切换（个人/家庭/合伙） -->
    <scroll-view scroll-x class="wh-bar" v-if="warehouses.length">
      <view v-for="w in warehouses" :key="w.id" class="wh-tag"
            :class="{ active: String(w.id) === String(currentId) }" @tap="switchWh(w)">
        <text class="wh-badge">{{ whTypeBadge(w.warehouseType) }}</text>
        <text class="wh-name">{{ w.name }}</text>
      </view>
      <view class="wh-tag add" @tap="openWhSheet()">＋ 新建</view>
    </scroll-view>
    <view v-else class="empty" @tap="openWhSheet()">还没有仓库，点此新建第一个仓库</view>

    <!-- 概览（对齐产品方案 4.9 仓库页结构） -->
    <view class="ov-card" v-if="currentId && overview && overview.productCount !== undefined">
      <view class="ov-row">
        <view class="ov-cell"><view class="ov-num">¥{{ fmt(overview.stockValue) }}</view><view class="ov-label">库存总价值</view></view>
        <view class="ov-cell"><view class="ov-num">{{ overview.totalQuantity || 0 }}</view><view class="ov-label">待售件数</view></view>
        <view class="ov-cell"><view class="ov-num">{{ overview.productCount || 0 }}</view><view class="ov-label">商品数</view></view>
      </view>
      <view class="ov-row">
        <view class="ov-cell"><view class="ov-num">{{ overview.shelfCount || 0 }}</view><view class="ov-label">货架</view></view>
        <view class="ov-cell"><view class="ov-num">{{ overview.monthInQuantity || 0 }}</view><view class="ov-label">本月入库</view></view>
        <view class="ov-cell"><view class="ov-num">{{ overview.monthOutQuantity || 0 }}</view><view class="ov-label">本月出库</view></view>
      </view>
      <view class="ov-tip" v-if="overview.zeroStockCount > 0">⚠ {{ overview.zeroStockCount }} 个商品零库存，可考虑补货</view>
      <view class="ov-tip" v-if="overview.expiredCount > 0">⛔ {{ overview.expiredCount }} 个商品已过期，建议尽快处理</view>
      <view class="ov-tip warn" v-if="overview.expiringCount > 0">⏳ {{ overview.expiringCount }} 个商品 30 天内到期</view>
      <view class="ov-more" @tap="goReport">查看仓库报表（销售 / 库龄 / 风险）›</view>
    </view>

    <!-- 货架 / 商品 切换（放在报表下面） -->
    <view class="seg-tabs" v-if="currentId">
      <view class="seg-tab" :class="{ on: tab === 'shelf' }" @tap="switchTab('shelf')">货架</view>
      <view class="seg-tab" :class="{ on: tab === 'goods' }" @tap="switchTab('goods')">商品</view>
    </view>

    <!-- 货架列表（存放位置：归属仓库 + 区域/排/层/位 + 图片） -->
    <view class="card" v-if="currentId && tab === 'shelf'">
      <view class="card-title">
        <text class="flex-1">货架 · 存放位置</text>
        <text class="link" @tap="openShelfSheet()">＋新增货架</text>
      </view>
      <view v-if="!shelves.length" class="empty">还没有货架，点「＋新增货架」添加存放位置</view>
      <view class="shelf-grid">
        <view v-for="s in shelves" :key="s.id" class="shelf-cell" @tap="openShelfDetail(s)">
          <view class="shelf-cover">
            <image v-if="s.coverUrl" :src="s.coverUrl" class="shelf-img" mode="aspectFill" />
            <text v-else class="shelf-code">{{ s.code }}</text>
          </view>
          <view class="shelf-name">{{ s.name }}</view>
          <view class="shelf-pos">{{ s.locationText || '未设置位置' }}</view>
          <view class="shelf-meta">{{ s.productCount || 0 }} 种 · {{ s.stockQuantity || 0 }} 件</view>
        </view>
      </view>
    </view>

    <!-- 商品列表（带库存） -->
    <view class="card" v-if="currentId && tab === 'goods'">
      <view class="card-title">
        <text class="flex-1">商品</text>
        <text class="link" @tap="openFlows()">流水</text>
        <text class="link" style="margin-left: 16rpx" @tap="openProductSheet()">＋新增商品</text>
      </view>

      <!-- 查找：关键字（名称/条码）+ 类目筛选 -->
      <view class="search-row">
        <input class="search-input" v-model="keyword" placeholder="搜商品名 / 条码"
               confirm-type="search" @confirm="doSearch" />
        <text class="search-btn" @tap="doSearch">搜索</text>
      </view>
      <scroll-view scroll-x class="cat-bar">
        <view class="cat-chip" :class="{ on: !categoryId }" @tap="pickCategoryFilter(null)">全部</view>
        <view v-for="c in productCategories" :key="c.id" class="cat-chip"
              :class="{ on: String(categoryId) === String(c.id) }"
              @tap="pickCategoryFilter(c)">{{ c.name }}</view>
        <view class="cat-chip manage" @tap="openCatManage">⚙ 管理</view>
      </scroll-view>

      <!-- 货架筛选 -->
      <scroll-view scroll-x class="cat-bar">
        <view class="cat-chip" :class="{ on: !shelfFilterId }" @tap="pickShelfFilter(null)">全部货架</view>
        <view v-for="s in shelves" :key="s.id" class="cat-chip"
              :class="{ on: String(shelfFilterId) === String(s.id) }"
              @tap="pickShelfFilter(s)">{{ s.name }}</view>
      </scroll-view>

      <view v-if="!products.length" class="empty">{{ (keyword || categoryId || shelfFilterId) ? '没有匹配的商品' : '还没有商品，点「＋新增商品」添加' }}</view>
      <view v-for="p in products" :key="p.id" class="prod-row" @tap="openProductDetail(p)">
        <image v-if="p.coverUrl" :src="p.coverUrl" class="prod-img" mode="aspectFill" />
        <view v-else class="prod-img prod-img-ph">{{ (p.name || '?')[0] }}</view>
        <view class="flex-1 min-w-0">
          <view class="row-name">{{ p.name }}</view>
          <view class="row-sub">
            {{ p.categoryName || '未分类' }} · {{ p.unit || '个' }}
            <text v-if="p.tags"> · {{ p.tags }}</text>
          </view>
          <view class="row-sub2">
            <text>成本 ¥{{ fmt(p.costPrice) }}</text>
            <text v-if="p.sellPrice"> · 售价 ¥{{ fmt(p.sellPrice) }}</text>
            <text v-if="p.expiryDate"> · 到期 {{ String(p.expiryDate).slice(0,10) }}</text>
          </view>
        </view>
        <view class="row-right">
          <view class="qty" :class="{ warn: (p.stockQuantity || 0) === 0 }">{{ p.stockQuantity || 0 }} {{ p.unit || '个' }}</view>
          <view class="price">¥{{ fmt((p.costPrice || 0) * (p.stockQuantity || 0)) }}</view>
        </view>
      </view>

      <!-- 分页 -->
      <view class="pager" v-if="pageTotal > pageSize">
        <text class="pager-btn" :class="{ dis: pageNum <= 1 }" @tap="turnPage(-1)">‹</text>
        <text class="pager-info">{{ pageNum }} / {{ Math.ceil(pageTotal / pageSize) }} · 共 {{ pageTotal }}</text>
        <text class="pager-btn" :class="{ dis: pageNum * pageSize >= pageTotal }" @tap="turnPage(1)">›</text>
      </view>
    </view>

    <!-- 新建仓库弹层 -->
    <view class="mask" v-if="whSheet" @tap="whSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">新建仓库</view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="whForm.name" placeholder="如：我的小店" /></view>
        <view class="f-row"><text class="f-label">模式</text>
          <picker :range="whTypeNames" @change="onWhTypeChange">
            <view class="f-value">{{ whTypeNames[whTypeIndex] }} ▾</view>
          </picker></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="whSheet = false">取消</view>
          <view class="btn" @tap="saveWh">保存</view>
        </view>
      </view>
    </view>

    <!-- 新增商品弹层 -->
    <view class="mask" v-if="productSheet" @tap="productSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ pForm.id ? '编辑商品' : '新增商品' }}</view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="pForm.name" placeholder="商品名"
                 @blur="checkSimilar" @confirm="checkSimilar" /></view>

        <!-- 相似商品推荐：命中就直接入库到已有商品，不再重复建档 -->
        <view v-if="similarList.length" class="similar-box">
          <view class="similar-title">已有相似商品 · 直接入库可避免重复建档</view>
          <view v-for="s in similarList" :key="s.productId" class="similar-item" @tap="reuseSimilar(s)">
            <view class="flex-1 min-w-0">
              <view class="row-name">{{ s.name }}</view>
              <view class="row-sub">现有 {{ s.stockQuantity }} 件<text v-if="s.shelves && s.shelves.length"> · {{ s.shelves[0].shelfName }}</text></view>
            </view>
            <text class="similar-pick">入库 ›</text>
          </view>
        </view>

        <view class="f-row"><text class="f-label">类目</text>
          <picker :range="categoryPickerNames" @change="onProductCategoryChange">
            <view class="f-value">{{ pForm.categoryName || '未分类' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">成本价</text>
          <input class="f-input" type="digit" v-model="pForm.costPrice" placeholder="选填" /></view>
        <view class="f-row"><text class="f-label">售价</text>
          <input class="f-input" type="digit" v-model="pForm.sellPrice" placeholder="选填" /></view>
        <view class="f-row"><text class="f-label">单位</text>
          <input class="f-input" v-model="pForm.unit" placeholder="个 / 箱 / 件" /></view>
        <view class="f-row"><text class="f-label">条码</text>
          <input class="f-input" v-model="pForm.barcode" placeholder="选填，可扫码" />
          <text class="scan-btn" @tap="scanBarcode">扫码</text></view>
        <view class="f-row"><text class="f-label">成色</text>
          <picker :range="conditionNames" @change="onConditionChange">
            <view class="f-value">{{ pForm.conditionGrade || '全新' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">来源</text>
          <picker :range="channelNames" @change="onChannelChange">
            <view class="f-value">{{ pForm.sourceChannel || '未设置' }} ▾</view>
          </picker></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="productSheet = false">取消</view>
          <view class="btn" @tap="saveProduct">保存</view>
        </view>
      </view>
    </view>

    <!-- 出入库弹层 -->
    <view class="mask" v-if="stockTarget" @tap="stockTarget = null">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">出入库 · {{ stockTarget.name }}</view>
        <view class="sheet-sub">当前库存 {{ stockTarget.stockQuantity || 0 }}</view>
        <view class="f-row">
          <text class="f-label">方向</text>
          <view class="seg">
            <view class="seg-item" :class="{ on: stockMode === 'in' }" @tap="stockMode = 'in'">入库 +</view>
            <view class="seg-item" :class="{ on: stockMode === 'out' }" @tap="stockMode = 'out'">出库 −</view>
            <view class="seg-item" :class="{ on: stockMode === 'adjust' }" @tap="stockMode = 'adjust'">校准</view>
          </view>
        </view>
        <view class="f-row">
          <text class="f-label">货架</text>
          <picker :range="stockShelfNames" @change="onStockShelfChange">
            <view class="f-value">{{ stockShelfLabel }} ▾</view>
          </picker>
        </view>
        <view class="f-row">
          <text class="f-label">原因</text>
          <picker :range="reasonNames" @change="onStockReasonChange">
            <view class="f-value">{{ reasonLabel }} ▾</view>
          </picker>
        </view>
        <view class="f-row">
          <text class="f-label">{{ qtyLabelOf(stockMode) }}</text>
          <input class="f-input" type="number" v-model="stockQty" placeholder="0" />
        </view>
        <view class="f-row">
          <text class="f-label">备注</text>
          <input class="f-input" v-model="stockRemark" placeholder="选填" />
        </view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="stockTarget = null">取消</view>
          <view class="btn" @tap="saveStock">确认</view>
        </view>
      </view>
    </view>

    <!-- 货架新增/编辑 -->
    <view class="mask" v-if="shelfSheet" @tap="shelfSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ shelfForm.id ? '编辑货架' : '新增货架' }}</view>
        <view class="f-row"><text class="f-label">编号</text>
          <input class="f-input" v-model="shelfForm.code" placeholder="如 A-01（同仓唯一）" /></view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="shelfForm.name" placeholder="如 A区第1排" /></view>
        <view class="f-row"><text class="f-label">区域</text>
          <input class="f-input" v-model="shelfForm.zone" placeholder="如 A区 / 冷藏区" /></view>
        <view class="f-row">
          <text class="f-label">排层位</text>
          <input class="f-input" type="number" v-model="shelfForm.rowNo" placeholder="排" />
          <input class="f-input" type="number" v-model="shelfForm.layerNo" placeholder="层" />
          <input class="f-input" type="number" v-model="shelfForm.positionNo" placeholder="位" />
        </view>
        <view class="f-row"><text class="f-label">类型</text>
          <picker :range="shelfTypeNames" @change="onShelfTypeChange">
            <view class="f-value">{{ shelfTypeNameOf(shelfForm.shelfType) }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">容量</text>
          <input class="f-input" type="number" v-model="shelfForm.capacity" placeholder="选填，件" /></view>
        <view class="f-row"><text class="f-label">备注</text>
          <input class="f-input" v-model="shelfForm.remark" placeholder="选填" /></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="shelfSheet = false">取消</view>
          <view class="btn" @tap="saveShelf">保存</view>
        </view>
      </view>
    </view>

    <!-- 货架详情（图片 / 位置 / 归属 / 商品明细） -->
    <view class="mask" v-if="shelfDetail.visible" @tap="shelfDetail.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ shelfDetail.shelf.name }} · {{ shelfDetail.shelf.code }}</view>

        <!-- 图片（1:N，首张封面） -->
        <scroll-view scroll-x class="detail-imgs">
          <view v-for="(img, i) in shelfDetail.images" :key="img.id" class="detail-img-wrap">
            <image :src="img.fileUrl" class="detail-img" mode="aspectFill" @tap.stop="previewImg(img.fileUrl)" />
            <text v-if="i === 0" class="detail-img-cover">封面</text>
            <text class="detail-img-del" @tap.stop="removeShelfImage(img)">×</text>
          </view>
          <view class="detail-img-add" @tap="uploadShelfImage">＋</view>
        </scroll-view>

        <view class="f-row"><text class="f-label">位置</text>
          <text class="f-value">{{ shelfDetail.shelf.locationText || '未设置' }}</text></view>
        <view class="f-row"><text class="f-label">归属</text>
          <text class="f-value">{{ shelfDetail.shelf.warehouseName }} · {{ shelfTypeNameOf(shelfDetail.shelf.shelfType) }}</text></view>
        <view class="f-row"><text class="f-label">容量</text>
          <text class="f-value">{{ shelfDetail.shelf.capacity ? shelfDetail.shelf.capacity + ' 件' : '不限' }}</text></view>
        <view class="f-row" v-if="shelfDetail.overCapacity">
          <text class="f-label"></text>
          <text class="over-tip">已超容量（仅提示，不影响使用）</text>
        </view>

        <view class="sheet-sub">本货架 · {{ shelfDetail.totalQuantity }} 件 · ¥{{ fmt(shelfDetail.totalCostValue) }}</view>
        <scroll-view scroll-y class="detail-items">
          <view v-if="!shelfDetail.items.length" class="empty">该货架暂无库存</view>
          <view v-for="it in shelfDetail.items" :key="it.productId" class="row" @tap="openProductDetail({ id: it.productId })">
            <view class="flex-1 min-w-0">
              <view class="row-name">{{ it.productName }}</view>
              <view class="row-sub">{{ it.unit || '个' }} · 成本 ¥{{ fmt(it.costPrice) }}</view>
            </view>
            <view class="row-right">
              <view class="qty">{{ it.quantity }} 件</view>
              <view class="price">¥{{ fmt(it.costValue) }}</view>
            </view>
          </view>
        </scroll-view>

        <view class="sheet-actions">
          <view class="btn ghost" @tap="editShelfFromDetail">编辑</view>
          <view class="btn ghost" @tap="openTransfer">移库</view>
          <view class="btn" @tap="deleteShelfFromDetail">删除货架</view>
        </view>
      </view>
    </view>

    <!-- 移库（货架之间搬运） -->
    <view class="mask" v-if="transferForm.visible" @tap="transferForm.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">移库</view>
        <view class="f-row"><text class="f-label">商品</text>
          <picker :range="transferItemNames" @change="onTransferProduct">
            <view class="f-value">{{ transferForm.itemName || '选择本货架商品' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">从</text>
          <text class="f-value">{{ shelfDetail.shelf.name }}</text></view>
        <view class="f-row"><text class="f-label">到</text>
          <picker :range="transferTargetNames" @change="onTransferTarget">
            <view class="f-value">{{ transferForm.targetName || '选择目标货架' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">数量</text>
          <input class="f-input" type="number" v-model="transferForm.quantity" placeholder="0" /></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="transferForm.visible = false">取消</view>
          <view class="btn" @tap="saveTransfer">确认移库</view>
        </view>
      </view>
    </view>

    <!-- 商品详情（图片轮播 + 各货架库存分布） -->
    <view class="mask" v-if="productDetail.visible" @tap="productDetail.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ productDetail.product.name }}</view>

        <scroll-view scroll-x class="detail-imgs">
          <view v-for="(img, i) in productDetail.images" :key="img.id" class="detail-img-wrap">
            <image :src="img.fileUrl" class="detail-img" mode="aspectFill" @tap.stop="previewImg(img.fileUrl)" />
            <text v-if="i === 0" class="detail-img-cover">封面</text>
            <text class="detail-img-del" @tap.stop="removeProductImage(img)">×</text>
          </view>
          <view class="detail-img-add" @tap="uploadProductImage">＋</view>
        </scroll-view>

        <view class="f-row"><text class="f-label">分类</text>
          <text class="f-value">{{ productDetail.product.category || '未分类' }}</text></view>
        <view class="f-row"><text class="f-label">单位</text>
          <text class="f-value">{{ productDetail.product.unit || '个' }}</text></view>
        <view class="f-row" v-if="productDetail.product.barcode"><text class="f-label">条码</text>
          <text class="f-value">{{ productDetail.product.barcode }}</text></view>
        <view class="f-row" v-if="productDetail.product.conditionGrade"><text class="f-label">成色</text>
          <text class="f-value">{{ productDetail.product.conditionGrade }}</text></view>
        <view class="f-row" v-if="productDetail.product.sourceChannel"><text class="f-label">来源</text>
          <text class="f-value">{{ productDetail.product.sourceChannel }}</text></view>
        <view class="f-row"><text class="f-label">成本价</text>
          <text class="f-value">¥{{ fmt(productDetail.product.costPrice) }}</text></view>
        <view class="f-row"><text class="f-label">售价</text>
          <text class="f-value">¥{{ fmt(productDetail.product.sellPrice) }}</text></view>
        <view class="f-row"><text class="f-label">总库存</text>
          <text class="f-value">{{ productDetail.totalQuantity }} 件 · ¥{{ fmt(productDetail.totalCostValue) }}</text></view>
        <!-- 盘点补充：保质期 / 包装 / 标签 / 描述 / 动销 -->
        <view class="f-row" v-if="productDetail.product.productionDate"><text class="f-label">生产日期</text>
          <text class="f-value">{{ productDetail.product.productionDate }}</text></view>
        <view class="f-row" v-if="productDetail.product.shelfLifeDays"><text class="f-label">保质期</text>
          <text class="f-value">{{ shelfLifeText(productDetail.product.shelfLifeDays) }}</text></view>
        <view class="f-row" v-if="productDetail.product.expiryDate"><text class="f-label">到期日期</text>
          <view class="exp-wrap">
            <text class="f-value">{{ productDetail.product.expiryDate }}</text>
            <text v-if="expiryStatus(productDetail.product.expiryDate)" class="exp-tag" :class="{ bad: expiryStatus(productDetail.product.expiryDate).bad }">{{ expiryStatus(productDetail.product.expiryDate).text }}</text>
          </view></view>
        <view class="f-row" v-if="productDetail.product.packagingType && productDetail.product.packagingType !== 'none'"><text class="f-label">包装</text>
          <text class="f-value">{{ packText(productDetail.product.packagingType) }}</text></view>
        <view class="f-row" v-if="productDetail.product.tags"><text class="f-label">标签</text>
          <text class="f-value">{{ productDetail.product.tags }}</text></view>
        <view class="f-row" v-if="productDetail.product.description"><text class="f-label">描述</text>
          <text class="f-value">{{ productDetail.product.description }}</text></view>
        <view class="f-row" v-if="productDetail.product.lastMovedAt"><text class="f-label">最后动销</text>
          <text class="f-value">{{ (productDetail.product.lastMovedAt || '').slice(0, 10) }}<text v-if="productDetail.product.slowDays !== null && productDetail.product.slowDays !== undefined" class="f-sub">（{{ productDetail.product.slowDays }} 天前）</text></text></view>

        <view class="sheet-sub">�� 盘点：实盘数量 vs 账面</view>

        <view class="sheet-sub">库存分布（按货架）</view>
        <scroll-view scroll-y class="detail-items">
          <view v-if="!productDetail.shelves.length" class="empty">暂无库存，可「出入库」后查看分布</view>
          <view v-for="(r, i) in productDetail.shelves" :key="i" class="row" @tap="goShelf(r)">
            <view class="flex-1 min-w-0">
              <view class="row-name">{{ r.shelfName }}<text v-if="r.shelfCode" class="row-code"> · {{ r.shelfCode }}</text></view>
              <view class="row-sub">{{ r.locationText || '未设置位置' }}</view>
            </view>
            <view class="row-right">
              <view class="qty">{{ r.quantity }} 件</view>
              <view class="price">¥{{ fmt(r.costValue) }}</view>
            </view>
          </view>
        </scroll-view>

        <view class="sheet-actions">
          <view class="btn ghost" @tap="editProductFromDetail">编辑</view>
          <view class="btn" @tap="stockFromProductDetail">出入库</view>
        </view>
      </view>
    </view>

    <!-- 库存流水（谁、何时、做了什么） -->
    <view class="mask" v-if="flowSheet" @tap="flowSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">库存流水（{{ flowTotal }} 条）</view>
        <scroll-view scroll-y class="detail-items">
          <view v-if="!flows.length" class="empty">还没有流水，出入库后这里会出现记录</view>
          <view v-for="f in flows" :key="f.id" class="fl-row">
            <view class="fl-badge" :class="{ inn: f.quantityChange > 0 }">{{ f.flowTypeLabel || f.flowType }}</view>
            <view class="flex-1 min-w-0">
              <view class="row-name">{{ f.productName }}<text v-if="f.reasonLabel" class="fl-reason"> · {{ f.reasonLabel }}</text></view>
              <view class="row-sub">{{ f.shelfName }}<text v-if="f.channelName"> · {{ f.channelName }}</text><text v-if="f.toShelfName"> → {{ f.toShelfName }}</text> · {{ f.createTime }}</view>
            </view>
            <view class="fl-qty" :class="{ inn: f.quantityChange > 0 }">
              {{ f.quantityChange > 0 ? '+' : '' }}{{ f.quantityChange }}
            </view>
          </view>
        </scroll-view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="flowSheet = false">关闭</view>
          <view class="btn" @tap="loadFlows">刷新</view>
        </view>
      </view>
    </view>

  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listWarehouses, createWarehouse, listProducts, createProduct, stockIn, stockOut,
  listShelves, createShelf, updateShelf, deleteShelf, getShelfDetail,
  attachShelfImage, deleteInventoryImage, uploadFile, transferStock, getProductDetail,
  attachProductImage, updateProduct,
  listProductCategories, createProductCategory, deleteProductCategory,
  findSimilarProducts, stocktake, getWarehouseOverview, listStockFlows } from '@/api/ledger';

const WH_TYPES = [
  ['personal', '个人仓'],
  ['family', '家庭仓'],
  ['partner', '合伙仓']
];

/** 货架类型（与后端 LedgerShelf.TYPE_* 对齐） */
// 出入库原因（对应产品方案 4.3「状态管理：在库/已售/自用/损耗/转出」）
const IN_REASONS = [['purchase', '采购'], ['gift', '赠送'], ['other', '其他']];
const OUT_REASONS = [['sale', '销售'], ['self_use', '自用'], ['loss', '损耗'], ['transfer', '转出'], ['gift', '赠送'], ['other', '其他']];

const CONDITIONS = ['全新', '95新', '9成新', '8成新', '7成新及以下'];
const CHANNELS = ['线下进货', '淘宝', '拼多多', '1688', '闲鱼', '抖音', '小红书', '其他'];
const SHELF_TYPES = [
  ['normal', '普通货架'],
  ['refrigerated', '冷藏/冷冻'],
  ['locked', '保险柜/上锁'],
  ['temp', '临时暂存']
];

export default {
  components: { NavBar },
  data() {
    return {
      warehouses: [],
      currentId: null,
      products: [],
      whSheet: false,
      productSheet: false,
      whForm: { name: '', type: 'personal' },
      whTypeIndex: 0,
      pForm: { name: '', category: '', costPrice: '', sellPrice: '' },
      stockTarget: null,
      stockMode: 'in',
      stockQty: '',
      stockRemark: '',
      // 下单（销售/采购）：赊账自动建应收/应付 + 库存联动
      // ===== 货架（存放位置）=====
      tab: 'goods',
      shelves: [],
      shelfSheet: false,
      shelfForm: {
        id: null, code: '', name: '', zone: '',
        rowNo: '', layerNo: '', positionNo: '',
        shelfType: 'normal', capacity: '', remark: ''
      },
      shelfDetail: {
        visible: false, shelf: {}, items: [], images: [],
        totalQuantity: 0, totalCostValue: 0, overCapacity: false
      },
      transferForm: { visible: false, productId: null, itemName: '', targetShelfId: null, targetName: '', quantity: '' },
      // 商品详情（图片 + 各货架库存分布）
      // 商品图片（轻量：只保留一个图片列表）
      flowSheet: false,
      flows: [],
      flowTotal: 0,
      productDetail: { visible: false, product: {}, shelves: [], images: [], totalQuantity: 0, totalCostValue: 0 },
      // ===== 查找 / 类目 / 相似商品 =====
      keyword: '',
      pageNum: 1,
      pageSize: 20,
      pageTotal: 0,
      categoryId: null,
      productCategories: [],
      catManageSheet: false,
      newCatName: '',
      similarList: [],
      reuseForm: { visible: false, productId: null, name: '', shelfId: 0, shelfName: '未上架', quantity: '' },
      // ===== 概览 / 盘点 / 出入库原因 =====
      overview: {},
      stockReason: '',
      // 出入库所选货架（0/null = 未上架）
      stockShelfId: 0
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    whTypeNames() { return WH_TYPES.map(t => t[1]); },
    productNames() { return this.products.map(p => p.name); },
    shelfTypeNames() { return SHELF_TYPES.map(t => t[1]); }, conditionNames() { return CONDITIONS; }, channelNames() { return CHANNELS; }, /** 移库可选商品（本货架上还有货的） */
    transferItemNames() { return (this.shelfDetail.items || []).map(it => it.productName); },
    /** 移库可选目标货架（排除当前货架） */
    transferTargetList() {
      const cur = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      return this.shelves.filter(s => String(s.id) !== String(cur));
    },
    transferTargetNames() {
      return this.transferTargetList.map(s => s.name + (s.code ? ' · ' + s.code : ''));
    },
    /** 出入库货架选择器（只列真实货架） */
    stockShelfNames() {
      return this.shelves.map(s => s.name + (s.code ? ' · ' + s.code : ''));
    },
    stockShelfLabel() {
      if (!this.stockShelfId) return '请选择货架';
      const s = this.shelves.find(x => String(x.id) === String(this.stockShelfId));
      return s ? s.name + (s.code ? ' · ' + s.code : '') : '请选择货架';
    },
    /** 类目选择器：一级 + 二级（二级显示「一级 / 二级」） */
    categoryPickerList() {
      const out = [];
      for (const c of this.productCategories) {
        out.push({ id: c.id, name: c.name });
        for (const k of (c.children || [])) {
          out.push({ id: k.id, name: c.name + ' / ' + k.name });
        }
      }
      return out;
    },
    categoryPickerNames() { return this.categoryPickerList.map(x => x.name); },
    /** 出入库原因候选 */
    reasonList() { return this.stockMode === 'in' ? IN_REASONS : OUT_REASONS; },
    reasonNames() { return this.reasonList.map(x => x[1]); },
    reasonLabel() {
      const x = this.reasonList.find(i => i[0] === this.stockReason);
      if (x) return x[1];
      return this.stockMode === 'in' ? '采购' : '销售';
    },
    /** 盘点页货架选择（首项「未上架」） */
    /** 复用入库的货架选择（首项「未上架」） */
    reuseShelfNames() {
      return ['未上架'].concat(this.shelves.map(s => s.name + (s.code ? ' · ' + s.code : '')));
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadWarehouses();
  },
  methods: {
    /** 点图片全屏预览 */
    previewImg(url) {
      if (url) uni.previewImage({ urls: [url] });
    },
    fmt(v) { return Number(v || 0).toFixed(2); },
    // ===== 货架（存放位置）=====
    switchTab(t) {
      this.tab = t;
      if (t === 'shelf') this.loadShelves();
      else this.loadProducts();
    },
    shelfTypeNameOf(t) {
      const x = SHELF_TYPES.find(i => i[0] === t);
      return x ? x[1] : '普通货架';
    },
    onConditionChange(e) { this.pForm.conditionGrade = CONDITIONS[e.detail.value] || ''; }, onChannelChange(e) { this.pForm.sourceChannel = CHANNELS[e.detail.value] || ''; }, onShelfTypeChange(e) {
      const x = SHELF_TYPES[e.detail.value];
      if (x) this.shelfForm.shelfType = x[0];
    },
    /** 出入库所选货架（首项=未上架） */
    onStockShelfChange(e) {
      const i = Number(e.detail.value);
      this.stockShelfId = i <= 0 ? 0 : this.shelves[i - 1].id;
    },
    /** 出入库原因 */
    onStockReasonChange(e) {
      const x = this.reasonList[e.detail.value];
      this.stockReason = x ? x[0] : '';
    },
    /** 进仓库报表 */
    goReport() { uni.navigateTo({ url: '/pages/warehouse/report/index' }); },
    // ===== 库存流水 =====
    async openFlows() {
      if (!this.currentId) { uni.showToast({ title: '请先选择仓库', icon: 'none' }); return; }
      this.flowSheet = true;
      await this.loadFlows();
    },
    async loadFlows() {
      try {
        const res = (await listStockFlows(this.currentId, { pageNum: 1, pageSize: 50 })) || {};
        this.flows = res.records || [];
        this.flowTotal = res.total || 0;
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 扫码填条码 */
    scanBarcode() {
      uni.scanCode({
        success: (r) => {
          const code = (r.result || '').trim();
          if (code) {
            this.pForm.barcode = code;
            uni.showToast({ title: '已识别条码', icon: 'success' });
            this.checkSimilar();
          }
        }
      });
    },
    async loadOverview() {
      if (!this.currentId) { this.overview = {}; return; }
      try {
        this.overview = (await getWarehouseOverview(this.currentId)) || {};
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ===== 查找 / 类目 / 相似商品 =====
    doSearch() { this.pageNum = 1; this.loadProducts(); },
    pickCategoryFilter(c) {
      this.categoryId = c ? c.id : null;
      this.pageNum = 1;
      this.loadProducts();
    },
    async loadProductCategories() {
      try {
        this.productCategories = (await listProductCategories()) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    /**
     * 名称输入后查重：有相似商品就在弹层里提示「直接入库到已有商品」，
     * 避免同一件货建两条档案、库存被拆散。
     */
    async checkSimilar() {
      const name = (this.pForm.name || '').trim();
      if (name.length < 2 || !this.currentId) { this.similarList = []; return; }
      try {
        this.similarList = (await findSimilarProducts(this.currentId, name, this.pForm.id)) || [];
      } catch (e) { this.similarList = []; }
    },
    /** 选中已有商品 → 转为「入库到该商品」，不新建档案 */
    reuseSimilar(s) {
      this.productSheet = false;
      this.similarList = [];
      this.reuseForm = {
        visible: true,
        productId: s.productId,
        name: s.name,
        shelfId: 0,
        shelfName: '未上架',
        quantity: ''
      };
      this.loadShelves();
    },
    onReuseShelfChange(e) {
      const i = Number(e.detail.value);
      if (i <= 0) {
        this.reuseForm.shelfId = 0;
        this.reuseForm.shelfName = '未上架';
      } else {
        const s = this.shelves[i - 1];
        if (!s) return;
        this.reuseForm.shelfId = s.id;
        this.reuseForm.shelfName = s.name + (s.code ? ' · ' + s.code : '');
      }
    },
    async saveReuseStock() {
      const f = this.reuseForm;
      const qty = Number(f.quantity);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }
      try {
        await stockIn({
          warehouseId: this.currentId,
          productId: f.productId,
          shelfId: f.shelfId || 0,
          quantity: qty
        });
        uni.showToast({ title: '已入库', icon: 'success' });
        f.visible = false;
        await this.loadProducts();
        await this.loadShelves();
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 新增商品时选择类目 */
    onProductCategoryChange(e) {
      const c = this.categoryPickerList[e.detail.value];
      if (!c) return;
      this.pForm.categoryId = c.id;
      this.pForm.categoryName = c.name;
    },
    // ===== 类目管理 =====
    openCatManage() {
      this.newCatName = '';
      this.catManageSheet = true;
      this.loadProductCategories();
    },
    async addProductCategory() {
      const name = (this.newCatName || '').trim();
      if (!name) { uni.showToast({ title: '请输入类目名', icon: 'none' }); return; }
      try {
        await createProductCategory({ name, parentId: 0 });
        this.newCatName = '';
        await this.loadProductCategories();
        uni.showToast({ title: '已添加', icon: 'success' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeProductCategory(c) {
      uni.showModal({
        title: '删除类目',
        content: '删除「' + c.name + '」？有子类目或商品引用时会被拒绝',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteProductCategory(c.id);
            await this.loadProductCategories();
            await this.loadProducts();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    async loadShelves() {
      if (!this.currentId) { this.shelves = []; return; }
      try {
        this.shelves = (await listShelves(this.currentId)) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 打开新增/编辑货架（传 s 即编辑） */
    openShelfSheet(s) {
      if (!this.currentId) { uni.showToast({ title: '请先新建/选择仓库', icon: 'none' }); return; }
      this.shelfForm = s ? {
        id: s.id, code: s.code || '', name: s.name || '', zone: s.zone || '',
        rowNo: s.rowNo == null ? '' : s.rowNo,
        layerNo: s.layerNo == null ? '' : s.layerNo,
        positionNo: s.positionNo == null ? '' : s.positionNo,
        shelfType: s.shelfType || 'normal',
        capacity: s.capacity == null ? '' : s.capacity,
        remark: s.remark || ''
      } : {
        id: null, code: '', name: '', zone: '',
        rowNo: '', layerNo: '', positionNo: '',
        shelfType: 'normal', capacity: '', remark: ''
      };
      this.shelfSheet = true;
    },
    async saveShelf() {
      const f = this.shelfForm;
      if (!(f.code || '').trim()) { uni.showToast({ title: '请填货架编号', icon: 'none' }); return; }
      if (!(f.name || '').trim()) { uni.showToast({ title: '请填货架名称', icon: 'none' }); return; }
      const payload = {
        warehouseId: this.currentId,
        code: f.code.trim(),
        name: f.name.trim(),
        zone: (f.zone || '').trim() || null,
        rowNo: f.rowNo === '' ? null : Number(f.rowNo),
        layerNo: f.layerNo === '' ? null : Number(f.layerNo),
        positionNo: f.positionNo === '' ? null : Number(f.positionNo),
        shelfType: f.shelfType,
        capacity: f.capacity === '' ? null : Number(f.capacity),
        remark: (f.remark || '').trim() || null
      };
      try {
        if (f.id) await updateShelf(f.id, payload);
        else await createShelf(payload);
        uni.showToast({ title: f.id ? '已保存' : '已创建', icon: 'success' });
        this.shelfSheet = false;
        await this.loadShelves();
        if (this.shelfDetail.visible && f.id) await this.openShelfDetail({ id: f.id });
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 货架详情（含图片 + 商品明细） */
    async openShelfDetail(s) {
      try {
        const d = (await getShelfDetail(s.id)) || {};
        this.shelfDetail = {
          visible: true,
          shelf: d.shelf || {},
          items: d.items || [],
          images: d.images || [],
          totalQuantity: d.totalQuantity || 0,
          totalCostValue: d.totalCostValue || 0,
          overCapacity: !!d.overCapacity
        };
      } catch (e) { /* 拦截器已提示 */ }
    },
    editShelfFromDetail() {
      const s = this.shelfDetail.shelf;
      if (!s || !s.id) return;
      this.shelfDetail.visible = false;
      this.openShelfSheet(s);
    },
    deleteShelfFromDetail() {
      const s = this.shelfDetail.shelf;
      if (!s || !s.id) return;
      uni.showModal({
        title: '删除货架',
        content: '删除「' + s.name + '」？仍有库存时会被拒绝，请先移库',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteShelf(s.id);
            this.shelfDetail.visible = false;
            await this.loadShelves();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 上传货架图片（先传文件拿 URL，再挂到货架上，1:N） */
    uploadShelfImage() {
      const id = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      if (!id) return;
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          uni.showLoading({ title: '上传中', mask: true });
          try {
            const url = await uploadFile(path, 'ledger_shelf');
            if (url) {
              await attachShelfImage(id, url);
              await this.openShelfDetail({ id });
              await this.loadShelves();
            }
            uni.hideLoading();
          } catch (e) {
            uni.hideLoading();
          }
        }
      });
    },
    removeShelfImage(img) {
      const id = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      if (!img || !img.id) return;
      uni.showModal({
        title: '删除图片',
        content: '确定删除这张图片？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteInventoryImage(img.id);
            await this.openShelfDetail({ id });
            await this.loadShelves();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    openTransfer() {
      if (!this.shelfDetail.items.length) {
        uni.showToast({ title: '该货架暂无库存，无法移库', icon: 'none' }); return;
      }
      if (!this.transferTargetList.length) {
        uni.showToast({ title: '请先再建一个货架作为目标', icon: 'none' }); return;
      }
      this.transferForm = { visible: true, productId: null, itemName: '', targetShelfId: null, targetName: '', quantity: '' };
    },
    onTransferProduct(e) {
      const it = this.shelfDetail.items[e.detail.value];
      if (!it) return;
      this.transferForm.productId = it.productId;
      this.transferForm.itemName = it.productName;
    },
    onTransferTarget(e) {
      const s = this.transferTargetList[e.detail.value];
      if (!s) return;
      this.transferForm.targetShelfId = s.id;
      this.transferForm.targetName = s.name + (s.code ? ' · ' + s.code : '');
    },
    async saveTransfer() {
      const f = this.transferForm;
      if (!f.productId) { uni.showToast({ title: '请选择商品', icon: 'none' }); return; }
      if (!f.targetShelfId) { uni.showToast({ title: '请选择目标货架', icon: 'none' }); return; }
      const qty = Number(f.quantity);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }
      try {
        await transferStock({
          warehouseId: this.currentId,
          productId: f.productId,
          fromShelfId: this.shelfDetail.shelf.id,
          toShelfId: f.targetShelfId,
          quantity: qty
        });
        uni.showToast({ title: '已移库', icon: 'success' });
        f.visible = false;
        await this.openShelfDetail({ id: this.shelfDetail.shelf.id });
        await this.loadShelves();
        await this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ===== 商品详情（图片轮播 + 各货架库存分布）=====
    async openProductDetail(p) {

      try {
        const d = (await getProductDetail(p.id)) || {};
        this.productDetail = {
          visible: true,
          product: d.product || p,
          shelves: d.shelves || [],
          images: d.images || [],
          totalQuantity: d.totalQuantity || 0,
          totalCostValue: d.totalCostValue || 0
        };
      } catch (e) { /* 拦截器已提示 */ }
    },
    uploadProductImage() {
      const id = this.productDetail.product && this.productDetail.product.id;
      if (!id) return;
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          uni.showLoading({ title: '上传中', mask: true });
          try {
            const kind = 'product';
            const bizType = 'ledger_product';
            const url = await uploadFile(path, bizType);
            if (url) {
              await attachProductImage(id, url, kind);
              await this.openProductDetail({ id });
              await this.loadProducts();
            }
            uni.hideLoading();
          } catch (e) { uni.hideLoading(); }
        }
      });
    },
    removeProductImage(img) {
      const id = this.productDetail.product && this.productDetail.product.id;
      if (!img || !img.id) return;
      uni.showModal({
        title: '删除图片',
        content: '确定删除这张图片？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteInventoryImage(img.id);
            await this.openProductDetail({ id });
            await this.loadProducts();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 编辑商品：跳到入库页的编辑模式（同一套属性表单） */
    editProductFromDetail() {
      const p = this.productDetail.product;
      if (!p || !p.id) return;
      this.productDetail.visible = false;
      uni.navigateTo({ url: '/pages/warehouse/inout/index?edit=' + p.id });
    },
    /** 盘点：直接把该商品库存改成实际数量（差异写盘盈/盘亏流水） */
    stocktakeProduct() {
      const p = this.productDetail.product;
      if (!p || !p.id) return;
      uni.showModal({
        title: '盘点：实际数量',
        editable: true,
        placeholderText: '当前 ' + (p.stockQuantity || 0),
        success: async (r) => {
          if (!r.confirm) return;
          const n = Number(String(r.content || '').trim());
          if (isNaN(n) || n < 0) { uni.showToast({ title: '请输入有效数量', icon: 'none' }); return; }
          const top = (this.productDetail.shelves || []).slice()
            .sort((a, b) => (b.quantity || 0) - (a.quantity || 0))[0];
          try {
            await stocktake({
              warehouseId: this.currentId,
              shelfId: top ? top.shelfId : 0,
              items: [{ productId: p.id, actualQuantity: n }],
              remark: '盘点'
            });
            uni.showToast({ title: '已盘点', icon: 'success' });
            this.productDetail.visible = false;
            await this.loadProducts();
            await this.loadShelves();
            await this.loadOverview();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 出入库：跳到入库页的出库模式（同一套交互） */
    stockFromProductDetail() {
      const p = this.productDetail.product;
      if (!p || !p.id) return;
      this.productDetail.visible = false;
      uni.navigateTo({ url: '/pages/warehouse/inout/index?out=' + p.id });
    },
    whTypeText(t) { const x = WH_TYPES.find(i => i[0] === t); return x ? x[1] : ''; },
    async loadWarehouses() {
      try {
        const res = await listWarehouses() || [];
        this.warehouses = res || [];
        if (this.warehouses.length && (!this.currentId || !this.warehouses.find(w => w.id === this.currentId))) {
          this.currentId = this.warehouses[0].id;
          this.loadProducts();
          this.loadShelves();
          this.loadProductCategories();
          this.loadOverview();
        } else if (!this.warehouses.length) {
          this.currentId = null;
          this.products = [];
          this.shelves = [];
        }
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 切换仓库：概览/货架/商品全部重载，并清掉跨仓失效的筛选 */
    async switchWh(w) {
      if (String(w.id) === String(this.currentId)) return;
      this.currentId = w.id;
      this.keyword = '' ;
      this.categoryId = null;
      this.pageNum = 1;
      this.products = [];
      this.shelves = [];
      await Promise.all([this.loadShelves(), this.loadProducts(), this.loadOverview()]);
    },
    async loadProducts() {
      try {
        const params = { pageNum: this.pageNum || 1, pageSize: this.pageSize || 20 };
        if ((this.keyword || '').trim()) params.keyword = this.keyword.trim();
        if (this.categoryId) params.categoryId = this.categoryId;
        if (this.shelfFilterId) params.shelfId = this.shelfFilterId;
        const res = await listProducts(this.currentId, params) || {};
        this.products = res.records || [];
        this.pageTotal = res.total || 0;
      } catch (e) { /* 拦截器已提示 */ }
    },
    openWhSheet() { this.whForm = { name: '', type: 'personal' }; this.whTypeIndex = 0; this.whSheet = true; },
    /** 商品详情的库存分布 → 跳到对应货架详情（shelfId=0 表示未上架，无处可跳） */
    goShelf(r) {
      if (!r || !r.shelfId) { uni.showToast({ title: '该部分库存在「未上架」', icon: 'none' }); return; }
      this.productDetail.visible = false;
      this.openShelfDetail({ id: r.shelfId });
    },    openStock(p, shelfId) {
      this.stockTarget = p;
      this.stockShelfId = shelfId || 0;
      this.loadShelves();
      this.stockMode = 'in';
      this.stockQty = '';
      this.stockRemark = '';
      this.stockReason = '';
    },
    /** 数量标签：校准态叫「实际数量」 */
    qtyLabelOf(mode) { return mode === 'adjust' ? '实际数量' : '数量'; },
    /** 出入库三态：入库 + / 出库 − / 校准（直接设为实际数量，差异记盘点流水） */
    async saveStock() {
      const raw = String(this.stockQty == null ? '' : this.stockQty).trim();
      const qty = parseInt(raw, 10);
      const isAdjust = this.stockMode === 'adjust';
      if (isAdjust) {
        if (raw === '' || isNaN(qty) || qty < 0) {
          uni.showToast({ title: '请输入实际数量', icon: 'none' });
          return;
        }
      } else if (!qty || qty <= 0) {
        uni.showToast({ title: '请输入数量', icon: 'none' });
        return;
      }
      try {
        if (isAdjust) {
          // 校准 = 盘点：把该货架该商品直接设为 qty，差异由后端写 adjust 流水
          const res = (await stocktake({
            warehouseId: this.currentId,
            shelfId: this.stockShelfId || 0,
            items: [{ productId: this.stockTarget.id, actualQuantity: qty }],
            remark: this.stockRemark || '校准'
          })) || {};
          const d = (res.details && res.details[0]) || null;
          uni.showToast({
            title: d ? ('已校准：' + d.before + ' → ' + d.actual) : '数量未变',
            icon: 'none'
          });
          this.stockTarget = null;
          this.loadProducts();
          this.loadShelves();
          this.loadOverview();
          return;
        }
        const fn = this.stockMode === 'in' ? stockIn : stockOut;
        await fn({
          warehouseId: this.currentId,
          productId: this.stockTarget.id,
          shelfId: this.stockShelfId || 0,
          reason: this.stockReason || undefined,
          quantity: qty,
          remark: this.stockRemark || undefined
        });
        uni.showToast({ title: this.stockMode === 'in' ? '已入库' : '已出库', icon: 'success' });
        this.stockTarget = null;
        this.loadProducts();
        this.loadShelves();
        this.loadOverview();
      } catch (e) { /* 拦截器已提示 */ }
    },
    openProductSheet() {
      this.pForm = {
        id: null, name: '', categoryId: null, categoryName: '', unit: '', barcode: '', conditionGrade: '', sourceChannel: '',
        costPrice: '', sellPrice: ''
      };
      this.similarList = [];
      this.productSheet = true;
    },
    onWhTypeChange(e) { this.whTypeIndex = e.detail.value; this.whForm.type = WH_TYPES[e.detail.value][0]; },
    async saveWh() {
      if (!this.whForm.name.trim()) { uni.showToast({ title: '请填名称', icon: 'none' }); return; }
      try {
        await createWarehouse({ name: this.whForm.name.trim(), warehouseType: this.whForm.type });
        uni.showToast({ title: '已创建', icon: 'success' });
        this.whSheet = false;
        this.loadWarehouses();
      } catch (e) { /* 拦截器已提示 */ }
    },
    async saveProduct() {
      if (!this.pForm.name.trim()) { uni.showToast({ title: '请填商品名', icon: 'none' }); return; }
      const payload = {
        warehouseId: this.currentId,
        name: this.pForm.name.trim(),
        categoryId: this.pForm.categoryId || null,
        costPrice: this.pForm.costPrice ? Number(this.pForm.costPrice) : null,
        sellPrice: this.pForm.sellPrice ? Number(this.pForm.sellPrice) : null, unit: (this.pForm.unit || '').trim() || null, barcode: (this.pForm.barcode || '').trim() || null, conditionGrade: this.pForm.conditionGrade || null, sourceChannel: this.pForm.sourceChannel || null
      };
      try {
        if (this.pForm.id) {
          await updateProduct(this.pForm.id, payload);
          uni.showToast({ title: '已保存', icon: 'success' });
        } else {
          await createProduct(payload);
          uni.showToast({ title: '已添加', icon: 'success' });
        }
        this.productSheet = false;
        this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.wh-bar { white-space: nowrap; padding: 20rpx 24rpx 0; }
.wh-tag { display: inline-block; padding: 12rpx 28rpx; margin-right: 16rpx; border-radius: 32rpx; font-size: 24rpx; background: var(--card); color: var(--text-sub); }
.wh-tag.active { background: var(--primary-soft); color: var(--primary-text); font-weight: 600; }
.wh-tag.add { color: var(--primary); border: 1rpx dashed var(--primary); background: transparent; }
.empty { text-align: center; color: var(--text-muted); padding: 80rpx 0; font-size: 26rpx; }
.card { background: var(--card); border-radius: 20rpx; margin: 20rpx 24rpx 0; padding: 8rpx 32rpx; }
.card-title { display: flex; align-items: center; padding: 20rpx 0; font-size: 28rpx; color: var(--text-main); font-weight: 600; }
.flex-1 { flex: 1; }
.link { color: var(--primary); font-size: 24rpx; }
.row { display: flex; align-items: center; padding: 22rpx 0; border-bottom: 1rpx solid var(--border); }
.row:last-child { border-bottom: none; }
.row-name { font-size: 28rpx; color: var(--text-main); }
.row-sub { margin-top: 4rpx; font-size: 22rpx; color: var(--text-muted); }
.row-right { text-align: right; flex-shrink: 0; margin-left: 16rpx; }
.qty { font-size: 26rpx; font-weight: 600; color: var(--income); }
.qty.warn { color: var(--text-muted); }
.price { font-size: 20rpx; color: var(--text-sub); margin-top: 4rpx; }
.mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.sheet { width: 620rpx; background: var(--card); border-radius: 24rpx; padding: 32rpx; }
.sheet-title { font-size: 30rpx; font-weight: 600; text-align: center; color: var(--text-main); margin-bottom: 16rpx; }
.sheet-sub { font-size: 24rpx; color: var(--text-sub); text-align: center; margin-bottom: 12rpx; }
.seg { display: flex; background: var(--surface); border-radius: 12rpx; overflow: hidden; }
.seg-item { flex: 1; text-align: center; padding: 14rpx 0; font-size: 26rpx; color: var(--text-sub); }
.seg-item.on { background: var(--primary-soft); color: var(--primary-text); font-weight: 600; }
.f-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid var(--border); }
.f-label { width: 150rpx; font-size: 26rpx; color: var(--text-main); flex-shrink: 0; }
.f-input { flex: 1; min-width: 0; font-size: 26rpx; text-align: right; color: var(--text-main); }
.f-value { font-size: 26rpx; color: var(--text-main); }
.sheet-actions { display: flex; gap: 20rpx; margin-top: 28rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 44rpx; font-size: 28rpx; background: var(--primary); color: var(--on-primary); }
.btn.ghost { background: var(--surface); color: var(--text-sub); }
/* ===== 下单明细行 ===== */
.order-line {
  display: flex; align-items: center; gap: 10rpx;
  padding: 12rpx 0; border-bottom: 1rpx solid var(--border);
}
.order-pick { flex: 1.4; font-size: 24rpx; }
.order-qty { flex: 0.7; }
.order-price { flex: 0.9; }
.order-del {
  font-size: 32rpx; color: #E85D5D; padding: 0 8rpx; line-height: 1;
}
/* ===== 货架 / 商品 切换 ===== */
.seg-tabs {
  display: flex; gap: 12rpx; padding: 16rpx 24rpx 0;
}
.seg-tab {
  flex: 1; text-align: center; padding: 16rpx 0; border-radius: 12rpx;
  font-size: 28rpx; color: var(--text-sub); background: var(--card);
  border: 1rpx solid var(--border);
}
.seg-tab.on {
  color: #fff; background: var(--primary); border-color: var(--primary); font-weight: 600;
}

/* ===== 货架网格 ===== */
.shelf-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx;
  padding: 8rpx 0;
}
.shelf-cell {
  border: 1rpx solid var(--border); border-radius: 14rpx; overflow: hidden;
  background: var(--card);
}
.shelf-cover {
  height: 150rpx; display: flex; align-items: center; justify-content: center;
  background: var(--bg);
}
.shelf-img { width: 100%; height: 100%; }
.shelf-code {
  font-size: 34rpx; font-weight: 700; color: var(--primary); letter-spacing: 1rpx;
}
.shelf-name {
  font-size: 27rpx; font-weight: 600; color: var(--text-main);
  padding: 10rpx 14rpx 2rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.shelf-pos {
  font-size: 22rpx; color: var(--text-sub); padding: 0 14rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.shelf-meta {
  font-size: 22rpx; color: var(--text-sub); padding: 4rpx 14rpx 12rpx;
}

/* ===== 货架详情 ===== */
.detail-imgs { white-space: nowrap; padding: 10rpx 0 16rpx; }
.detail-img-wrap { display: inline-block; position: relative; margin-right: 14rpx; }
.detail-img {
  width: 160rpx; height: 160rpx; border-radius: 12rpx; display: block;
}
.detail-img-cover {
  position: absolute; left: 0; bottom: 0; padding: 2rpx 10rpx;
  font-size: 20rpx; color: #fff; background: rgba(0,0,0,.55);
  border-bottom-left-radius: 12rpx; border-top-right-radius: 10rpx;
}
.detail-img-del {
  position: absolute; top: -6rpx; right: -6rpx;
  width: 34rpx; height: 34rpx; line-height: 30rpx; text-align: center;
  border-radius: 50%; background: #E85D5D; color: #fff; font-size: 24rpx;
}
.detail-img-add {
  display: inline-block; width: 160rpx; height: 160rpx; line-height: 160rpx;
  text-align: center; font-size: 44rpx; color: var(--text-sub);
  border: 1rpx dashed var(--border); border-radius: 12rpx; vertical-align: top;
}
.detail-items { max-height: 460rpx; }
.over-tip { font-size: 22rpx; color: #E8654A; }
.row-code { font-size: 22rpx; color: var(--text-sub); font-weight: 400; }
/* ===== 仓库概览卡片 ===== */
.ov-card {
  margin: 16rpx 24rpx 0; padding: 20rpx 16rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx;
}
.ov-row { display: flex; }
.ov-cell { flex: 1; text-align: center; padding: 6rpx 0; }
.ov-num { font-size: 30rpx; font-weight: 700; color: var(--text-main); }
.ov-label { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.ov-tip {
  margin-top: 10rpx; padding-top: 10rpx; border-top: 1rpx solid var(--border);
  font-size: 22rpx; color: #E8654A;
}

/* ===== 盘点行 ===== */
.st-row {
  display: flex; align-items: center; gap: 12rpx;
  padding: 12rpx 0; border-bottom: 1rpx solid var(--border);
}
.st-input {
  width: 140rpx; text-align: center; font-size: 26rpx;
  border: 1rpx solid var(--border); border-radius: 8rpx; padding: 6rpx 0;
}
/* ===== 扫码按钮 / 流水 ===== */
.scan-btn {
  flex: 0 0 auto; padding: 8rpx 18rpx; margin-left: 8rpx;
  font-size: 24rpx; color: #fff; background: var(--primary); border-radius: 8rpx;
}
.fl-row {
  display: flex; align-items: center; gap: 12rpx;
  padding: 14rpx 0; border-bottom: 1rpx solid var(--border);
}
.fl-badge {
  flex: 0 0 auto; padding: 4rpx 12rpx; border-radius: 6rpx;
  font-size: 20rpx; color: #E8654A; background: rgba(232,101,74,.12);
}
.fl-badge.inn { color: #0EA37A; background: rgba(14,163,122,.12); }
.fl-qty { font-size: 30rpx; font-weight: 700; color: #E8654A; }
.fl-qty.inn { color: #0EA37A; }
.fl-reason { font-size: 22rpx; color: var(--text-sub); font-weight: 400; }
.ov-tip.warn { color: #E8A33D; }
.ov-more { margin-top: 10rpx; padding-top: 10rpx; border-top: 1rpx solid var(--border); font-size: 23rpx; color: var(--primary); text-align: center; }
/* ===== 仓库切换条（与入库/出库页一致） ===== */
.wh-bar { white-space: nowrap; padding: 12rpx 20rpx 0; text-align: center; }
.wh-tag {
  display: inline-flex; align-items: center; gap: 8rpx;
  padding: 8rpx 18rpx; margin-right: 10rpx;
  border-radius: 10rpx; font-size: 24rpx; color: var(--text-sub);
  background: var(--card); border: 1rpx solid var(--border);
}
.wh-tag.active { color: #fff; background: var(--primary); border-color: var(--primary); }
.wh-badge {
  font-size: 19rpx; line-height: 1; padding: 4rpx 7rpx; border-radius: 4rpx;
  background: rgba(95,167,122,.16); color: var(--primary); font-weight: 700;
}
.wh-tag.active .wh-badge { background: rgba(255,255,255,.28); color: #fff; }
.wh-name { font-size: 24rpx; }
.exp-wrap { flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 8rpx; }
.exp-tag { font-size: 20rpx; padding: 2rpx 8rpx; border-radius: 4rpx; background: rgba(232,163,61,.16); color: #E8A33D; }
.exp-tag.bad { background: rgba(232,101,74,.14); color: #E8654A; }
.f-sub { font-size: 21rpx; color: var(--text-sub); }
/* ===== 商品列表行（带图 + 多属性 + 分页） ===== */
.prod-row { display: flex; align-items: center; gap: 16rpx; padding: 18rpx 0; border-bottom: 1rpx solid var(--border); }
.prod-img { width: 88rpx; height: 88rpx; border-radius: 10rpx; flex: 0 0 auto; }
.prod-img-ph {
  display: flex; align-items: center; justify-content: center;
  background: rgba(95,167,122,.14); color: var(--primary);
  font-size: 36rpx; font-weight: 700;
}
.row-sub2 { font-size: 21rpx; color: var(--text-sub); margin-top: 2rpx; }
.pager { display: flex; align-items: center; justify-content: center; gap: 24rpx; padding: 20rpx 0 8rpx; }
.pager-btn { font-size: 34rpx; color: var(--primary); padding: 0 18rpx; }
.pager-btn.dis { color: var(--border); }
.pager-info { font-size: 23rpx; color: var(--text-sub); }
</style>
